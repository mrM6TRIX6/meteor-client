/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ScreenOpenEvent;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.clientsettings.ClientSettings;
import meteordevelopment.meteorclient.systems.modules.combat.*;
import meteordevelopment.meteorclient.systems.modules.exploit.*;
import meteordevelopment.meteorclient.systems.modules.fun.*;
import meteordevelopment.meteorclient.systems.modules.misc.*;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.Swarm;
import meteordevelopment.meteorclient.systems.modules.movement.*;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.systems.modules.player.*;
import meteordevelopment.meteorclient.systems.modules.render.*;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.BlockESP;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.world.*;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.ValueComparableMap;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Modules extends System<Modules> {
    
    private static final List<Category> CATEGORIES = new ArrayList<>();
    
    private final Map<Class<? extends Module>, Module> moduleInstances = new Reference2ReferenceOpenHashMap<>();
    private final Map<Category, List<Module>> groups = new Reference2ReferenceOpenHashMap<>();
    
    private final List<Module> active = new ArrayList<>();
    private Module moduleToBind;
    private boolean awaitingKeyRelease = false;
    
    public Modules() {
        super("modules", new File(MeteorClient.FOLDER, "modules.json"));
    }
    
    public static Modules get() {
        return Systems.get(Modules.class);
    }
    
    @Override
    public void init() {
        initCombat();
        initPlayer();
        initMovement();
        initRender();
        initWorld();
        initMisc();
        initExploit();
        initFun();
    }
    
    @Override
    public void load() {
        for (Module module : getAll()) {
            for (SettingGroup group : module.settings) {
                for (Setting<?> setting : group) {
                    setting.reset();
                }
            }
        }
        
        super.load();
    }
    
    public void sortModules() {
        for (List<Module> modules : groups.values()) {
            modules.sort(Comparator.comparing(module -> module.name));
        }
    }
    
    public static void registerCategory(Category category) {
        if (!Categories.isRegistering()) {
            throw new RuntimeException("Modules.registerCategory - Cannot register category outside of categories init.");
        }
        
        CATEGORIES.add(category);
    }
    
    public static Iterable<Category> loopCategories() {
        return CATEGORIES;
    }
    
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Module> T get(Class<T> clazz) {
        return (T) moduleInstances.get(clazz);
    }
    
    @SuppressWarnings("unused")
    public <T extends Module> Optional<T> getOptional(Class<T> clazz) {
        return Optional.ofNullable(get(clazz));
    }
    
    @Nullable
    public Module get(String name) {
        for (Module module : moduleInstances.values()) {
            if (module.name.equalsIgnoreCase(name)) {
                return module;
            }
        }
        
        return null;
    }
    
    public boolean isActive(Class<? extends Module> clazz) {
        Module module = get(clazz);
        return module != null && module.isActive();
    }
    
    public List<Module> getGroup(Category category) {
        return groups.computeIfAbsent(category, category1 -> new ArrayList<>());
    }
    
    public Collection<Module> getAll() {
        return moduleInstances.values();
    }
    
    public int getCount() {
        return moduleInstances.size();
    }
    
    public List<Module> getActive() {
        return active;
    }
    
    public List<Pair<Module, String>> searchNames(String text) {
        Map<Pair<Module, String>, Integer> modules = new HashMap<>();
        
        for (Module module : this.moduleInstances.values()) {
            String title = module.name;
            int score = Utils.searchLevenshteinDefault(title, text, false);
            
            if (ClientSettings.get().moduleAliases.get()) {
                for (String alias : module.aliases) {
                    int aliasScore = Utils.searchLevenshteinDefault(alias, text, false);
                    if (aliasScore < score) {
                        title = module.name + " (" + alias + ")";
                        score = aliasScore;
                    }
                }
            }
            
            modules.put(new Pair<>(module, title), score);
        }
        
        List<Pair<Module, String>> l = new ArrayList<>(modules.keySet());
        l.sort(Comparator.comparingInt(modules::get));
        
        return l;
    }
    
    public Set<Module> searchSettingTitles(String text) {
        Map<Module, Integer> modules = new ValueComparableMap<>(Comparator.naturalOrder());
        
        for (Module module : this.moduleInstances.values()) {
            int lowest = Integer.MAX_VALUE;
            for (SettingGroup sg : module.settings) {
                for (Setting<?> setting : sg) {
                    int score = Utils.searchLevenshteinDefault(setting.title, text, false);
                    if (score < lowest) {
                        lowest = score;
                    }
                }
            }
            modules.put(module, modules.getOrDefault(module, 0) + lowest);
        }
        
        return modules.keySet();
    }
    
    void addActive(Module module) {
        synchronized (active) {
            if (!active.contains(module)) {
                active.add(module);
                MeteorClient.EVENT_BUS.post(ActiveModulesChangedEvent.get());
            }
        }
    }
    
    void removeActive(Module module) {
        synchronized (active) {
            if (active.remove(module)) {
                MeteorClient.EVENT_BUS.post(ActiveModulesChangedEvent.get());
            }
        }
    }
    
    // Binding
    
    public void setModuleToBind(Module moduleToBind) {
        this.moduleToBind = moduleToBind;
    }
    
    /***
     * @see meteordevelopment.meteorclient.commands.commands.BindCommand
     * For ensuring we don't instantly bind the module to the enter key.
     */
    public void awaitKeyRelease() {
        this.awaitingKeyRelease = true;
    }
    
    public boolean isBinding() {
        return moduleToBind != null;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onKeyBinding(KeyEvent event) {
        if (event.action == KeyAction.RELEASE && onBinding(true, event.key(), event.modifiers())) {
            event.cancel();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onButtonBinding(MouseClickEvent event) {
        if (event.action == KeyAction.RELEASE && onBinding(false, event.button(), 0)) {
            event.cancel();
        }
    }
    
    private boolean onBinding(boolean isKey, int value, int modifiers) {
        if (!isBinding()) {
            return false;
        }
        
        if (awaitingKeyRelease) {
            if (!isKey || (value != GLFW.GLFW_KEY_ENTER && value != GLFW.GLFW_KEY_KP_ENTER)) {
                return false;
            }
            
            awaitingKeyRelease = false;
            return false;
        }
        
        if (moduleToBind.keybind.canBindTo(isKey, value, modifiers)) {
            moduleToBind.keybind.set(isKey, value, modifiers);
            moduleToBind.info("Bound to (highlight)%s(default).", moduleToBind.keybind);
        } else if (value == GLFW.GLFW_KEY_ESCAPE) {
            moduleToBind.keybind.set(Keybind.none());
            moduleToBind.info("Removed bind.");
        } else {
            return false;
        }
        
        MeteorClient.EVENT_BUS.post(ModuleBindChangedEvent.get(moduleToBind));
        moduleToBind = null;
        
        return true;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        if (event.action == KeyAction.REPEAT) {
            return;
        }
        onAction(true, event.key(), event.modifiers(), event.action == KeyAction.PRESS);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseClick(MouseClickEvent event) {
        if (event.action == KeyAction.REPEAT) {
            return;
        }
        onAction(false, event.button(), 0, event.action == KeyAction.PRESS);
    }
    
    private void onAction(boolean isKey, int value, int modifiers, boolean isPress) {
        if (mc.currentScreen != null || Input.isKeyPressed(GLFW.GLFW_KEY_F3)) {
            return;
        }
        
        for (Module module : moduleInstances.values()) {
            if (module.keybind.matches(isKey, value, modifiers) && (isPress || (module.toggleOnBindRelease && module.isActive()))) {
                module.toggle();
                module.sendToggledMsg();
            }
        }
    }
    
    // End of binding
    
    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onOpenScreen(ScreenOpenEvent event) {
        if (!Utils.canUpdate()) {
            return;
        }
        
        for (Module module : moduleInstances.values()) {
            if (module.toggleOnBindRelease && module.isActive()) {
                module.toggle();
                module.sendToggledMsg();
            }
        }
    }
    
    @EventHandler
    private void onGameJoin(GameJoinEvent event) {
        synchronized (active) {
            for (Module module : getAll()) {
                if (module.isActive() && !module.runInMainMenu) {
                    MeteorClient.EVENT_BUS.subscribe(module);
                    module.onActivate();
                }
            }
        }
    }
    
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        synchronized (active) {
            for (Module module : getAll()) {
                if (module.isActive() && !module.runInMainMenu) {
                    MeteorClient.EVENT_BUS.unsubscribe(module);
                    module.onDeactivate();
                }
            }
        }
    }
    
    public void disableAll() {
        synchronized (active) {
            for (Module module : getAll()) {
                module.disable();
            }
        }
    }
    
    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        JsonArray modulesArray = new JsonArray();
        
        for (Module module : getAll()) {
            JsonObject moduleJson = module.toJson();
            if (moduleJson != null) {
                modulesArray.add(moduleJson);
            }
        }
        
        jsonObject.add("modules", modulesArray);
        return jsonObject;
    }
    
    @Override
    public Modules fromJson(JsonObject jsonObject) {
        disableAll();
        JsonArray modulesArray = jsonObject.get("modules").getAsJsonArray();
        
        for (JsonElement moduleJsonElement : modulesArray) {
            JsonObject moduleJson = moduleJsonElement.getAsJsonObject();
            Module module = get(moduleJson.get("name").getAsString());
            if (module != null) {
                module.fromJson(moduleJson);
            }
        }
        
        return this;
    }
    
    // INIT MODULES
    
    public void add(Module module) {
        // Check if the module's category is registered
        if (!CATEGORIES.contains(module.category)) {
            throw new RuntimeException("Modules.add - Module's category was not registered.");
        }
        
        // Check if the module with that name not exists
        moduleInstances.values().forEach(existing -> {
            if (existing.name.equalsIgnoreCase(module.name)) {
                throw new IllegalArgumentException("Module with name '%s' already exists".formatted(module.name));
            }
        });
        
        // Add the module
        moduleInstances.put(module.getClass(), module);
        getGroup(module.category).add(module);
        
        // Register color settings for the module
        module.settings.registerColorSettings(module);
    }
    
    private void initCombat() {
        add(new Aimbot());
        add(new AnchorAura());
        add(new AntiAnvil());
        add(new AntiBed());
        add(new ArrowDamage());
        add(new ArrowDodge());
        add(new AttributeSwap());
        add(new AutoAnvil());
        add(new AutoArmor());
        add(new AutoTotem());
        add(new AutoWeapon());
        add(new AutoWeb());
        add(new BedAura());
        add(new BowAimbot());
        add(new BowSpam());
        add(new Confuse());
        add(new Criticals());
        add(new CrystalAura());
        add(new Hitboxes());
        add(new HoleFiller());
        add(new KillAura());
        add(new MaceKill());
        add(new Quiver());
        add(new SelfAnvil());
    }
    
    private void initPlayer() {
        add(new AbortBreaking());
        add(new AntiHunger());
        add(new AutoEat());
        add(new AutoExtinguish());
        add(new AutoClicker());
        add(new AutoFish());
        add(new AutoGap());
        add(new AutoHotbar());
        add(new AutoMend());
        add(new AutoReplenish());
        add(new AutoTool());
        add(new BreakDelay());
        add(new ChestSwap());
        add(new EXPThrower());
        add(new FakePlayer());
        add(new FastUse());
        add(new GhostHand());
        add(new InstantRebreak());
        add(new LiquidInteract());
        add(new MiddleClickExtra());
        add(new MultiActions());
        add(new NoInteract());
        add(new NoMiningTrace());
        add(new NoRotate());
        add(new NoStatusEffects());
        add(new PortalMenu());
        add(new PotionSaver());
        add(new Reach());
        add(new Rotation());
        add(new SpeedMine());
        add(new VehicleOneHit());
    }
    
    private void initMovement() {
        add(new AirJump());
        add(new AirWalk());
        add(new Anchor());
        add(new AntiAFK());
        add(new AntiVoid());
        add(new AutoClip());
        add(new AutoJump());
        add(new AutoWalk());
        add(new AutoWasp());
        add(new Blink());
        add(new BoatFly());
        add(new Boost());
        add(new ClickTP());
        add(new ElytraBoost());
        add(new ElytraFly());
        add(new EntityControl());
        add(new EntitySpeed());
        add(new ExtraElytra());
        add(new FastClimb());
        add(new Flight());
        add(new Glide());
        add(new GUIMove());
        add(new HighJump());
        add(new Jesus());
        add(new LongJump());
        add(new NoFall());
        add(new NoJumpDelay());
        add(new NoSlow());
        add(new Parkour());
        add(new ParkourBot());
        add(new ReverseStep());
        add(new SafeWalk());
        add(new Slippy());
        add(new Sneak());
        add(new Speed());
        add(new Spider());
        add(new Sprint());
        add(new Step());
        add(new TridentBoost());
        add(new Velocity());
    }
    
    private void initRender() {
        add(new Ambience());
        add(new BetterTooltips());
        add(new BlockSelection());
        add(new BossStack());
        add(new Breadcrumbs());
        add(new BreakIndicators());
        add(new CameraTweaks());
        add(new Chams());
        add(new CityESP());
        add(new EntityOwner());
        add(new ESP());
        add(new Freecam());
        add(new FreeLook());
        add(new Fullbright());
        add(new HandView());
        add(new HUD());
        add(new ItemPhysics());
        add(new ItemHighlight());
        add(new LogoutSpots());
        add(new NameProtect());
        add(new Nametags());
        add(new NoFOV());
        add(new NoRender());
        add(new BlockESP());
        add(new StorageESP());
        add(new Test());
        add(new TimeChanger());
        add(new Tracers());
        add(new Trail());
        add(new Trajectories());
        add(new VoidESP());
        add(new WallHack());
        add(new Xray());
        add(new Blur());
        add(new PopChams());
        add(new TunnelESP());
        add(new BetterTab());
    }
    
    private void initWorld() {
        add(new AirPlace());
        add(new AutoBreed());
        add(new AutoBrewer());
        add(new AutoNametag());
        add(new AutoShearer());
        add(new AutoSign());
        add(new Collisions());
        add(new EndermanLook());
        add(new Flamethrower());
        add(new LiquidFiller());
        add(new NoGhostBlocks());
        add(new Nuker());
        add(new PacketMine());
        add(new Scaffold());
        add(new Timer());
        
        if (BaritoneUtils.IS_AVAILABLE) {
            add(new Excavator());
            add(new InfinityMiner());
        }
    }
    
    private void initMisc() {
        add(new AntiPacketKick());
        add(new AutoChatGame());
        add(new AutoLog());
        add(new AutoReconnect());
        add(new AutoRespawn());
        add(new BetterChat());
        add(new BetterMinecraft());
        add(new BookBot());
        add(new CommandAura());
        add(new DiscordPresence());
        add(new InventoryTweaks());
        add(new MessageAura());
        add(new Notebot());
        add(new Notifier());
        add(new OnSightCommand());
        add(new PacketCanceller());
        add(new PacketDebugger());
        add(new SoundBlocker());
        add(new Spammer());
        add(new Swarm());
        add(new SpectatorTeleport());
    }
    
    private void initExploit() {
        add(new BungeeCordSpoof());
        add(new NoSignLimit());
        add(new OffhandCrash());
        add(new Phase());
        add(new PingSpoof());
        add(new ServerSpoof());
    }
    
    private void initFun() {
        add(new BadTrip());
        add(new CrossbowSpam());
        add(new CustomHead());
        add(new HandDerp());
        add(new RainbowArmor());
        add(new SexAura());
        add(new SkinDerp());
        add(new Stick());
        add(new Twerk());
    }
    
}
