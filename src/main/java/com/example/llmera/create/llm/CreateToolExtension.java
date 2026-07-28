package com.example.llmera.create.llm;

import com.example.llmera.create.network.CreateMachineConnectorBlockEntity;
import com.example.llmera.create.network.CreateKineticsBridgeBlockEntity;
import com.example.llmera.llm.ToolDefinition;
import com.example.llmera.llm.ToolExtension;
import com.example.llmera.llm.ToolInvocation;
import com.example.llmera.network.CodeNetworkScanner;
import com.example.llmera.network.IntelligentTransmitterBlockEntity;
import com.example.llmera.network.LlmToolMode;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CreateToolExtension {
    private static final Gson GSON = new Gson();
    private static final int MAX_ARRAY = 64;

    private static final ToolDefinition KINETICS_DIAGNOSTICS = new ToolDefinition(
            "create_kinetics_diagnostics",
            "Read a Create kinetic machine through an online bridge or powered machine connector. Reports speed, stress, capacity, over-stressed status, source info, and network details.",
            bridgeParameters());
    private static final ToolDefinition RECIPE_LOOKUP = new ToolDefinition(
            "create_recipe_lookup",
            "Query server-side Create recipe types for recipes producing an item. Supports crushing, milling, mixing, pressing, cutting, splashing, haunting, filling, emptying, deploying, sandpaper_polishing, compacting, and mechanical_crafting.",
            Map.ofEntries(
                    Map.entry("target_id", Map.of("type", "string", "description", "registry id of the item to produce")),
                    Map.entry("recipe_type", Map.of("type", "string", "description", "optional filter: crushing, milling, mixing, pressing, cutting, splashing, haunting, filling, emptying, deploying, sandpaper_polishing, compacting, mechanical_crafting")),
                    Map.entry("max_recipes", Map.of("type", "integer", "description", "maximum returned recipes 1..20")),
                    Map.entry("x", coordinate("optional bridge x")), Map.entry("y", coordinate("optional bridge y")), Map.entry("z", coordinate("optional bridge z"))));
    private static final ToolDefinition MACHINE_DIAGNOSTICS = new ToolDefinition(
            "create_machine_diagnostics",
            "Read a Create machine through an online bridge or powered machine connector. Reports kinetic stats plus goggle-style information if available.",
            bridgeParameters());
    private static final ToolDefinition CRAFTER_LOAD = new ToolDefinition(
            "create_crafter_load",
            "Transfer items from a container (chest, barrel, etc.) into a Create Mechanical Crafter. The crafter must be in IDLE or ACCEPTING phase. Use this to load ingredients for mechanical crafting recipes.",
            Map.ofEntries(
                    Map.entry("source_x", coordinate("container x")), Map.entry("source_y", coordinate("container y")), Map.entry("source_z", coordinate("container z")),
                    Map.entry("target_x", coordinate("crafter x")), Map.entry("target_y", coordinate("crafter y")), Map.entry("target_z", coordinate("crafter z")),
                    Map.entry("item_id", Map.of("type", "string", "description", "optional: specific item to take (e.g. 'minecraft:iron_ingot'). If omitted, takes first available item")),
                    Map.entry("amount", Map.of("type", "integer", "description", "optional: number of items to transfer, default 1"))));

    private static final String GUIDANCE = """
            Create integration tools:
              - Use create_kinetics_diagnostics for kinetic state: speed (RPM), stress impact, stress capacity, over-stressed status, and network info.
              - Use create_machine_diagnostics for detailed machine state including goggle-style information.
              - Use create_recipe_lookup for Create recipe types (crushing, milling, mixing, pressing, cutting, splashing, haunting, filling, emptying, deploying, sandpaper_polishing, compacting, mechanical_crafting).
              - Use create_crafter_load to transfer items from containers (chests, barrels) into Mechanical Crafters for mechanical crafting. Supply source container and target crafter coordinates.
              - A powered machine connector can expose up to 16 linked Create machines. Supply target_x, target_y, and target_z when more than one machine is available.
            """;

    private CreateToolExtension() {
    }

    public static ToolExtension extension() {
        return new ToolExtension(List.of(
                new ToolExtension.Tool(KINETICS_DIAGNOSTICS, Set.of(LlmToolMode.BUILD, LlmToolMode.PLAN), CreateToolExtension::execute),
                new ToolExtension.Tool(MACHINE_DIAGNOSTICS, Set.of(LlmToolMode.BUILD, LlmToolMode.PLAN), CreateToolExtension::execute),
                new ToolExtension.Tool(RECIPE_LOOKUP, Set.of(LlmToolMode.BUILD, LlmToolMode.PLAN), CreateToolExtension::execute),
                new ToolExtension.Tool(CRAFTER_LOAD, Set.of(LlmToolMode.BUILD), CreateToolExtension::execute)
        ), GUIDANCE);
    }

    private static Map<String, Object> bridgeParameters() {
        return Map.ofEntries(
                Map.entry("x", coordinate("optional bridge or connector x")),
                Map.entry("y", coordinate("optional bridge or connector y")),
                Map.entry("z", coordinate("optional bridge or connector z")),
                Map.entry("target_x", coordinate("optional connected machine x")),
                Map.entry("target_y", coordinate("optional connected machine y")),
                Map.entry("target_z", coordinate("optional connected machine z")));
    }

    private static Map<String, String> coordinate(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private static String execute(IntelligentTransmitterBlockEntity transmitter, ToolInvocation invocation) {
        return switch (invocation.name()) {
            case "create_kinetics_diagnostics" -> kineticsDiagnostics(transmitter, invocation);
            case "create_machine_diagnostics" -> machineDiagnostics(transmitter, invocation);
            case "create_recipe_lookup" -> recipeLookup(transmitter, invocation);
            case "create_crafter_load" -> crafterLoad(transmitter, invocation);
            default -> error("unknown Create tool: " + invocation.name());
        };
    }

    private static String kineticsDiagnostics(IntelligentTransmitterBlockEntity transmitter, ToolInvocation invocation) {
        BridgeRef ref = resolveBridge(transmitter, invocation, true);
        if (ref.error != null) return error(ref.error);
        if (!(ref.target instanceof KineticBlockEntity kinetic)) return error("bridge target is not a Create kinetic machine");

        JsonObject out = base("create_kinetics_diagnostics");
        out.add("bridge", position(ref.bridgePos));
        out.add("target", position(kinetic.getBlockPos()));
        out.addProperty("block_id", BuiltInRegistries.BLOCK.getKey(kinetic.getBlockState().getBlock()).toString());
        out.addProperty("name", kinetic.getDisplayName().getString());
        out.addProperty("class", kinetic.getClass().getSimpleName());

        JsonObject kinetics = new JsonObject();
        kinetics.addProperty("speed", kinetic.getSpeed());
        kinetics.addProperty("theoretical_speed", kinetic.getTheoreticalSpeed());
        kinetics.addProperty("is_source", kinetic.isSource());
        kinetics.addProperty("has_source", kinetic.hasSource());
        kinetics.addProperty("over_stressed", kinetic.isOverStressed());
        if (kinetic.hasNetwork()) {
            try {
                var network = kinetic.getOrCreateNetwork();
                kinetics.addProperty("network_id", network.id);
                kinetics.addProperty("network_size", network.getSize());
            } catch (Exception ignored) {
            }
        }
        if (kinetic.source != null) {
            kinetics.add("source", position(kinetic.source));
        }

        float stressApplied = kinetic.calculateStressApplied();
        float stressCapacity = kinetic.calculateAddedStressCapacity();
        kinetics.addProperty("stress_impact", stressApplied);
        kinetics.addProperty("stress_capacity", stressCapacity);

        IRotate.SpeedLevel minSpeed = null;
        if (kinetic.getBlockState().getBlock() instanceof IRotate rotate) {
            minSpeed = rotate.getMinimumRequiredSpeedLevel();
        }
        if (minSpeed != null) {
            kinetics.addProperty("min_required_speed_level", minSpeed.getSerializedName());
        }

        out.add("kinetics", kinetics);
        return GSON.toJson(out);
    }

    private static String machineDiagnostics(IntelligentTransmitterBlockEntity transmitter, ToolInvocation invocation) {
        BridgeRef ref = resolveBridge(transmitter, invocation, true);
        if (ref.error != null) return error(ref.error);
        if (!(ref.target instanceof KineticBlockEntity kinetic)) return error("bridge target is not a Create kinetic machine");

        JsonObject out = base("create_machine_diagnostics");
        out.add("bridge", position(ref.bridgePos));
        out.add("target", position(kinetic.getBlockPos()));
        out.addProperty("block_id", BuiltInRegistries.BLOCK.getKey(kinetic.getBlockState().getBlock()).toString());
        out.addProperty("name", kinetic.getDisplayName().getString());
        out.addProperty("class", kinetic.getClass().getSimpleName());

        JsonObject kinetics = new JsonObject();
        kinetics.addProperty("speed", kinetic.getSpeed());
        kinetics.addProperty("theoretical_speed", kinetic.getTheoreticalSpeed());
        kinetics.addProperty("is_source", kinetic.isSource());
        kinetics.addProperty("has_source", kinetic.hasSource());
        kinetics.addProperty("over_stressed", kinetic.isOverStressed());
        kinetics.addProperty("stress_impact", kinetic.calculateStressApplied());
        kinetics.addProperty("stress_capacity", kinetic.calculateAddedStressCapacity());
        out.add("kinetics", kinetics);

        JsonArray goggleInfo = new JsonArray();
        try {
            List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();
            kinetic.addToGoggleTooltip(tooltip, false);
            for (var line : tooltip) {
                goggleInfo.add(line.getString());
            }
        } catch (Exception e) {
            out.addProperty("goggle_error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        out.add("goggle_info", goggleInfo);

        return GSON.toJson(out);
    }

    private static String recipeLookup(IntelligentTransmitterBlockEntity transmitter, ToolInvocation invocation) {
        BridgeRef ref = resolveBridge(transmitter, invocation, false);
        if (ref.error != null) return error(ref.error);

        String id = stringArg(invocation, "target_id");
        ResourceLocation target;
        try {
            target = ResourceLocation.parse(id == null ? "" : id);
        } catch (RuntimeException e) {
            return error("target_id must be a valid resource location");
        }
        String recipeTypeFilter = stringArg(invocation, "recipe_type");
        int maxRecipes = clamp(intArg(invocation, "max_recipes", 20), 1, 20);

        List<RecipeType<?>> recipeTypes = getRecipeTypes(recipeTypeFilter);
        if (recipeTypes.isEmpty()) {
            return error("unknown recipe_type filter: " + recipeTypeFilter);
        }

        JsonArray recipes = new JsonArray();
        for (RecipeType<?> type : recipeTypes) {
            if (recipes.size() >= maxRecipes) break;
            for (RecipeHolder<?> holder : ref.level.getRecipeManager().getAllRecipesFor(type)) {
                if (recipes.size() >= maxRecipes) break;
                JsonObject recipeJson = describeRecipe(holder);
                if (recipeJson == null) continue;
                JsonArray outputs = recipeJson.getAsJsonArray("outputs");
                if (outputs == null) continue;
                boolean produces = false;
                for (var out : outputs) {
                    JsonObject outObj = out.getAsJsonObject();
                    if (target.toString().equals(outObj.get("id").getAsString())) {
                        produces = true;
                        break;
                    }
                }
                if (produces) {
                    recipes.add(recipeJson);
                }
            }
        }

        JsonObject out = base("create_recipe_lookup");
        out.add("bridge", position(ref.bridgePos));
        out.addProperty("target_id", target.toString());
        if (recipeTypeFilter != null) out.addProperty("recipe_type_filter", recipeTypeFilter);
        out.add("recipes", recipes);
        out.addProperty("recipe_count", recipes.size());
        out.addProperty("truncated", recipes.size() >= maxRecipes);
        return GSON.toJson(out);
    }

    private static String crafterLoad(IntelligentTransmitterBlockEntity transmitter, ToolInvocation invocation) {
        if (!(transmitter.getLevel() instanceof ServerLevel level)) return error("transmitter level is not a ServerLevel");

        Integer sourceX = integer(invocation.arguments().get("source_x"));
        Integer sourceY = integer(invocation.arguments().get("source_y"));
        Integer sourceZ = integer(invocation.arguments().get("source_z"));
        if (sourceX == null || sourceY == null || sourceZ == null) return error("source_x, source_y, and source_z are required");
        BlockPos sourcePos = new BlockPos(sourceX, sourceY, sourceZ);

        Integer targetX = integer(invocation.arguments().get("target_x"));
        Integer targetY = integer(invocation.arguments().get("target_y"));
        Integer targetZ = integer(invocation.arguments().get("target_z"));
        if (targetX == null || targetY == null || targetZ == null) return error("target_x, target_y, and target_z are required");
        BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);

        if (!level.hasChunkAt(sourcePos)) return error("source position is not loaded");
        if (!level.hasChunkAt(targetPos)) return error("target position is not loaded");

        BlockEntity sourceBE = level.getBlockEntity(sourcePos);
        if (!(sourceBE instanceof Container sourceContainer)) return error("source is not a container");

        BlockEntity targetBE = level.getBlockEntity(targetPos);
        if (!(targetBE instanceof com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity crafter)) {
            return error("target is not a Mechanical Crafter");
        }

        String phase = crafter.getPhase().toString();
        if (!phase.equals("IDLE") && !phase.equals("ACCEPTING")) {
            return error("crafter is not ready (phase: " + phase + "). Must be IDLE or ACCEPTING");
        }

        String itemId = stringArg(invocation, "item_id");
        int amount = clamp(intArg(invocation, "amount", 1), 1, 64);

        ItemStack targetItem = ItemStack.EMPTY;
        if (itemId != null && !itemId.isEmpty()) {
            ResourceLocation itemRL = ResourceLocation.tryParse(itemId);
            if (itemRL == null) return error("invalid item_id: " + itemId);
            var itemHolder = BuiltInRegistries.ITEM.get(itemRL);
            if (itemHolder.isPresent()) {
                targetItem = new ItemStack(itemHolder.get(), amount);
            } else {
                return error("item not found: " + itemId);
            }
        }

        ItemStack taken = ItemStack.EMPTY;
        int transferred = 0;

        if (sourceBE instanceof ChestBlockEntity) {
            com.google.common.base.Preconditions.checkNotNull(sourceBE);
        }

        for (int i = 0; i < sourceContainer.getContainerSize(); i++) {
            ItemStack slot = sourceContainer.getItem(i);
            if (slot.isEmpty()) continue;

            if (!targetItem.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(slot, targetItem)) continue;
            }

            int toTake = Math.min(slot.getCount(), amount - transferred);
            if (toTake <= 0) continue;

            ItemStack extracted = slot.split(toTake);
            sourceContainer.setChanged();

            ItemStack remainder = crafter.getInventory().insertItem(0, extracted, false);
            crafter.setChanged();

            if (!remainder.isEmpty()) {
                slot.grow(remainder.getCount());
                sourceContainer.setChanged();
            }

            if (taken.isEmpty()) {
                taken = extracted.copy();
                taken.setCount(extracted.getCount() - remainder.getCount());
            } else {
                taken.grow(extracted.getCount() - remainder.getCount());
            }

            transferred += extracted.getCount() - remainder.getCount();
            if (transferred >= amount) break;
        }

        JsonObject out = base("create_crafter_load");
        out.add("source", position(sourcePos));
        out.add("target", position(targetPos));
        out.addProperty("crafter_phase", phase);
        if (taken.isEmpty()) {
            out.addProperty("transferred", 0);
            out.addProperty("message", "no matching items found in source container");
        } else {
            out.add("item", stackJson(taken));
            out.addProperty("transferred", transferred);
        }
        return GSON.toJson(out);
    }

    private static List<RecipeType<?>> getRecipeTypes(String filter) {
        if (filter == null || filter.isEmpty()) {
            return List.of(
                    AllRecipeTypes.CRUSHING.getType(),
                    AllRecipeTypes.MILLING.getType(),
                    AllRecipeTypes.MIXING.getType(),
                    AllRecipeTypes.COMPACTING.getType(),
                    AllRecipeTypes.PRESSING.getType(),
                    AllRecipeTypes.CUTTING.getType(),
                    AllRecipeTypes.SPLASHING.getType(),
                    AllRecipeTypes.HAUNTING.getType(),
                    AllRecipeTypes.DEPLOYING.getType(),
                    AllRecipeTypes.FILLING.getType(),
                    AllRecipeTypes.EMPTYING.getType(),
                    AllRecipeTypes.SANDPAPER_POLISHING.getType(),
                    AllRecipeTypes.MECHANICAL_CRAFTING.getType()
            );
        }
        return switch (filter.toLowerCase(Locale.ROOT)) {
            case "crushing" -> List.of(AllRecipeTypes.CRUSHING.getType());
            case "milling" -> List.of(AllRecipeTypes.MILLING.getType());
            case "mixing" -> List.of(AllRecipeTypes.MIXING.getType());
            case "compacting" -> List.of(AllRecipeTypes.COMPACTING.getType());
            case "pressing" -> List.of(AllRecipeTypes.PRESSING.getType());
            case "cutting" -> List.of(AllRecipeTypes.CUTTING.getType());
            case "splashing" -> List.of(AllRecipeTypes.SPLASHING.getType());
            case "haunting" -> List.of(AllRecipeTypes.HAUNTING.getType());
            case "deploying" -> List.of(AllRecipeTypes.DEPLOYING.getType());
            case "filling" -> List.of(AllRecipeTypes.FILLING.getType());
            case "emptying" -> List.of(AllRecipeTypes.EMPTYING.getType());
            case "sandpaper_polishing" -> List.of(AllRecipeTypes.SANDPAPER_POLISHING.getType());
            case "mechanical_crafting" -> List.of(AllRecipeTypes.MECHANICAL_CRAFTING.getType());
            default -> List.of();
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static JsonObject describeRecipe(RecipeHolder<?> holder) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("recipe_id", holder.id().toString());
            json.addProperty("type", BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()).toString());

            if (holder.value() instanceof ProcessingRecipe recipe) {
                JsonArray inputsJson = new JsonArray();
                JsonArray outputsJson = new JsonArray();

                for (Ingredient ingredient : recipe.getIngredients()) {
                    for (ItemStack stack : ingredient.getItems()) {
                        if (inputsJson.size() >= MAX_ARRAY) break;
                        JsonObject item = stackJson(stack);
                        item.addProperty("role", "input");
                        inputsJson.add(item);
                    }
                }

                for (ProcessingOutput output : recipe.getRollableResults()) {
                    if (outputsJson.size() >= MAX_ARRAY) break;
                    JsonObject item = stackJson(output.getStack());
                    item.addProperty("chance", output.getChance());
                    item.addProperty("role", "output");
                    outputsJson.add(item);
                }

                if (recipe.getProcessingDuration() > 0) {
                    json.addProperty("processing_duration", recipe.getProcessingDuration());
                }
                HeatCondition heat = recipe.getRequiredHeat();
                if (heat != HeatCondition.NONE) {
                    json.addProperty("required_heat", heat.getSerializedName());
                }

                json.add("inputs", inputsJson);
                json.add("outputs", outputsJson);
            } else {
                json.addProperty("unsupported_shape", holder.value().getClass().getSimpleName());
            }
            return json;
        } catch (Exception e) {
            JsonObject json = new JsonObject();
            json.addProperty("recipe_id", holder.id().toString());
            json.addProperty("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return json;
        }
    }

    private static BridgeRef resolveBridge(IntelligentTransmitterBlockEntity transmitter, ToolInvocation invocation,
                                           boolean requireUniqueTarget) {
        if (!(transmitter.getLevel() instanceof ServerLevel level)) return BridgeRef.failure("transmitter level is not a ServerLevel");
        CodeNetworkScanner.NetworkView view = CodeNetworkScanner.scan(level, transmitter.getBlockPos());
        List<BridgeRef> candidates = new ArrayList<>();
        Integer x = integer(invocation.arguments().get("x"));
        Integer y = integer(invocation.arguments().get("y"));
        Integer z = integer(invocation.arguments().get("z"));
        boolean anyCoordinate = x != null || y != null || z != null;
        if (anyCoordinate && (x == null || y == null || z == null)) return BridgeRef.failure("x, y, and z must be supplied together");
        BlockPos selectedBridge = anyCoordinate ? new BlockPos(x, y, z) : null;
        Integer targetX = integer(invocation.arguments().get("target_x"));
        Integer targetY = integer(invocation.arguments().get("target_y"));
        Integer targetZ = integer(invocation.arguments().get("target_z"));
        boolean anyTargetCoordinate = targetX != null || targetY != null || targetZ != null;
        if (anyTargetCoordinate && (targetX == null || targetY == null || targetZ == null)) {
            return BridgeRef.failure("target_x, target_y, and target_z must be supplied together");
        }
        BlockPos selectedTarget = anyTargetCoordinate ? new BlockPos(targetX, targetY, targetZ) : null;
        for (BlockPos member : view.memberPositions()) {
            if (!view.memberHasChannel(member)) continue;
            if (selectedBridge != null && !member.equals(selectedBridge)) continue;
            BlockEntity memberEntity = level.getBlockEntity(member);
            if (memberEntity instanceof CreateKineticsBridgeBlockEntity bridge) {
                BlockEntity target = bridge.target();
                if (selectedTarget != null && (target == null || !target.getBlockPos().equals(selectedTarget))) continue;
                if (CreateKineticsBridgeBlockEntity.isKineticTarget(target)) {
                    candidates.add(new BridgeRef(level, member.immutable(), target, null));
                }
                continue;
            }
            if (memberEntity instanceof CreateMachineConnectorBlockEntity connector && connector.isPowered()) {
                for (BlockPos targetPos : connector.getConnections()) {
                    if (selectedTarget != null && !targetPos.equals(selectedTarget)) continue;
                    if (!level.hasChunkAt(targetPos)) continue;
                    BlockEntity target = level.getBlockEntity(targetPos);
                    if (CreateMachineConnectorBlockEntity.isSupportedMachine(target)) {
                        candidates.add(new BridgeRef(level, member.immutable(), target, null));
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return BridgeRef.failure("no powered bridge or connector exposes a supported Create kinetic machine");
        }
        if (requireUniqueTarget && selectedTarget == null && candidates.size() > 1) {
            return BridgeRef.failure("multiple Create machines are connected; provide target_x, target_y, and target_z");
        }
        return candidates.getFirst();
    }

    private static JsonObject base(String tool) { JsonObject out = new JsonObject(); out.addProperty("ok", true); out.addProperty("tool", tool); return out; }
    private static String error(String message) { JsonObject out = new JsonObject(); out.addProperty("ok", false); out.addProperty("error", message); return GSON.toJson(out); }
    private static JsonObject position(BlockPos pos) { JsonObject out = new JsonObject(); out.addProperty("x", pos.getX()); out.addProperty("y", pos.getY()); out.addProperty("z", pos.getZ()); return out; }
    private static JsonObject stackJson(ItemStack value) { JsonObject out = new JsonObject(); out.addProperty("kind", "item"); out.addProperty("id", value.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(value.getItem()).toString()); out.addProperty("amount", value.getCount()); return out; }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static String stringArg(ToolInvocation invocation, String name) { Object value = invocation.arguments().get(name); return value == null ? null : value.toString().trim(); }
    private static int intArg(ToolInvocation invocation, String name, int fallback) { Integer value = integer(invocation.arguments().get(name)); return value == null ? fallback : value; }
    private static Integer integer(Object value) { return value instanceof Number number ? number.intValue() : value == null ? null : parseInteger(value.toString()); }
    private static Integer parseInteger(String value) { try { return Integer.valueOf(value); } catch (NumberFormatException e) { return null; } }

    private record BridgeRef(ServerLevel level, BlockPos bridgePos, BlockEntity target, String error) {
        static BridgeRef failure(String error) { return new BridgeRef(null, null, null, error); }
    }
}
