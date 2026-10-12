package xyz.daikirai.gui.opai;

import java.util.*;
import xyz.daikirai.client.*;
import xyz.daikirai.module.Category;
import xyz.daikirai.module.Module;
import xyz.daikirai.module.ModuleManager;
import xyz.daikirai.module.setting.*;
import net.minecraft.client.Minecraft;

/** Adapts existing B4 modules/profiles to Opai; it never substitutes gameplay implementations. */
public final class OpaiScreen {
    private final Minecraft mc=Minecraft.getMinecraft();
    private final OpaiUi ui=new OpaiUi(new Backend());
    private final Map<Module,Feature> features=new IdentityHashMap<>();
    private int width,height;
    public OpaiScreen() { ui.actions(()->mc.displayGuiScreen(null),()->mc.displayGuiScreen(new OpaiHudEditor())); }
    public void open(int width,int height) { sync();this.width=width;this.height=height;ui.open(width,height,OpaiMotion.now()); }
    private void sync() {
        for(OpaiUi.Column col:ui.columns) col.features.clear();
        for(Module module:ModuleManager.modules) {
            if(module.moduleCategory()==Category.profiles) continue;
            Category category=module.moduleCategory();
            int index=category==Category.combat?0:category==Category.movement?1:category==Category.player?2:category==Category.render?3:4;
            String name=new String[]{"Combat","Movement","Player","Visual","Misc"}[index];
            for(OpaiUi.Column col:ui.columns) if(col.name.equals(name)) col.features.add(features.computeIfAbsent(module,Feature::new));
        }
        for(OpaiUi.Column col:ui.columns) col.features.sort(Comparator.comparing(OpaiUi.Feature::name,String.CASE_INSENSITIVE_ORDER));
    }
    public void paint(int width,int height,int mx,int my) {
        if(this.width==0) open(width,height);
        if(this.width!=width || this.height!=height) {this.width=width;this.height=height;ui.resize(width,height,false);}
        ui.pencil(mc.thePlayer!=null);OpaiCanvas c=OpaiRuntime.canvas();
        OpaiRuntime.menuBackground();
        if(c.begin(width,height,mc.displayWidth,mc.displayHeight)) {
            try {ui.paint(c,OpaiRuntime.palette(),mx,my,OpaiMotion.now());} finally {c.end();}
        }
    }
    public void click(int x,int y,int button){ui.click(x,y,button,OpaiMotion.now());}
    public void release(){ui.release();}
    public void wheel(int x,int y,int amount,boolean shift){ui.wheel(x,y,amount,shift);}
    public void key(char character,int key){
        if(!ui.key(character,key,OpaiMotion.now()) && key==ModuleManager.gui.getKeycode()) ui.close(OpaiMotion.now());
    }
    public void close(){ui.release();}

    private static final class Feature implements OpaiUi.Feature {
        final Module module;final List<OpaiUi.Control> controls=new ArrayList<>();
        Feature(Module module) {this.module=module;for(Setting setting:module.getSettings()) controls.add(new Control(module,setting));}
        public String name(){return module.getName();}public boolean enabled(){return module.isEnabled();}
        public void toggle(){module.toggle();}public boolean bindable(){return module.canBeBound();}
        public void bind(int key){module.setBind(key);}public List<OpaiUi.Control> controls(){return controls;}
    }
    private static final class Control implements OpaiUi.Control {
        final Module module;final Setting setting;
        Control(Module module,Setting setting){this.module=module;this.setting=setting;}
        public String name(){return setting.getName();}
        public OpaiUi.Kind kind(){
            if(setting instanceof ButtonSetting)return ((ButtonSetting)setting).isMethodButton()?OpaiUi.Kind.ACTION:OpaiUi.Kind.BOOLEAN;
            if(setting instanceof SliderSetting)return ((SliderSetting)setting).isMode()?OpaiUi.Kind.MODE:OpaiUi.Kind.NUMBER;
            return OpaiUi.Kind.LABEL;
        }
        public double value(){return setting instanceof SliderSetting?((SliderSetting)setting).getInput():setting instanceof ButtonSetting&&((ButtonSetting)setting).isToggled()?1:0;}
        public double min(){return setting instanceof SliderSetting?((SliderSetting)setting).getMin():0;}
        public double max(){return setting instanceof SliderSetting?((SliderSetting)setting).getMax():1;}
        public String display(){return setting instanceof SliderSetting?((SliderSetting)setting).getDisplay(false).replaceAll("§.",""):"";}
        public String[] options(){return setting instanceof SliderSetting?((SliderSetting)setting).options:new String[0];}
        public void set(double value){
            if(setting instanceof SliderSetting)((SliderSetting)setting).setValue(value);
            else if(setting instanceof ButtonSetting){ButtonSetting button=(ButtonSetting)setting;button.toggle();module.guiButtonToggled(button);}
            module.guiUpdate();Profiles.markModified();
        }
    }
    private static final class Backend implements OpaiUi.Configurations {
        public List<String> names(){List<String> names=new ArrayList<>();for(Profile profile:Profiles.profiles.keySet())names.add(profile.name);return names;}
        public void refresh(){ /* Profile manager owns the loaded list and current profile identity. */ }
        private Profile find(String name){for(Profile p:Profiles.profiles.keySet())if(p.name.equals(name))return p;throw new IllegalStateException("Configuration not found");}
        public void load(String name){Profile p=find(name);if(!p.load())throw new IllegalStateException("Cannot load configuration");OpaiRuntime.success("Configuration Loaded",name+".json");}
        public void save(String name){if(!find(name).save())throw new IllegalStateException("Cannot save configuration");OpaiRuntime.success("Configuration Saved",name+".json");}
        public void delete(String name){Profile p=find(name);if(!new java.io.File(p.path).delete())throw new IllegalStateException("Cannot delete configuration");Profiles.remove(p,true,false);OpaiRuntime.success("Configuration Deleted",name+".json");}
        public void create(String name,boolean blank){
            if(!Profiles.validName(name)||!Profiles.create(name))throw new IllegalStateException("Cannot create configuration");
            if(blank){
                Profile profile=find(name);
                try {
                    com.google.gson.JsonObject root;
                    try(java.io.Reader reader=new java.io.InputStreamReader(new java.io.FileInputStream(profile.path),java.nio.charset.StandardCharsets.UTF_8)){root=new com.google.gson.JsonParser().parse(reader).getAsJsonObject();}
                    for(com.google.gson.JsonElement entry:root.getAsJsonArray("modules"))entry.getAsJsonObject().addProperty("enabled",false);
                    try(java.io.Writer writer=new java.io.OutputStreamWriter(new java.io.FileOutputStream(profile.path),java.nio.charset.StandardCharsets.UTF_8)){new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root,writer);}
                }catch(java.io.IOException error){throw new IllegalStateException("Cannot create blank configuration",error);}
            }
            OpaiRuntime.success("Configuration Created",name+".json");
        }
        public void folder(){
            java.io.File directory=new java.io.File(ClientConfig.DIR,"profiles");
            if(!directory.isDirectory()&&!directory.mkdirs())throw new IllegalStateException("Cannot create profile folder");
            try{if(java.awt.Desktop.isDesktopSupported())java.awt.Desktop.getDesktop().open(directory);else throw new IllegalStateException("Folder opening unavailable");}
            catch(java.io.IOException error){throw new IllegalStateException("Cannot open profile folder",error);}
        }
    }
}
