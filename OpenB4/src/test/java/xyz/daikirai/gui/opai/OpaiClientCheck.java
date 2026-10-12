package xyz.daikirai.gui.opai;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import xyz.daikirai.client.ClientConfig;
import xyz.daikirai.gui.ClickGui;
import xyz.daikirai.module.SettingsModule;
import xyz.daikirai.module.ModuleManager;

/** Test-only mod: real Minecraft/Forge context, local flat world, production adapters. */
@Mod(modid="opai_verify",name="Opai verification",version="1",clientSideOnly=true)
public final class OpaiClientCheck {
    private final Minecraft mc=Minecraft.getMinecraft();
    private int step;private long since;private OpaiUi ui;
    private final StringBuilder evidence=new StringBuilder();
    @Mod.EventHandler public void init(FMLInitializationEvent e){
        if(Boolean.getBoolean("raven.opai.clientCheck")){MinecraftForge.EVENT_BUS.register(this);since=System.currentTimeMillis();}
    }
    private void require(boolean value,String message){if(!value)throw new AssertionError(message);evidence.append("PASS ").append(message).append('\n');System.out.println("OPAI_CLIENT PASS "+message);}
    private void advance(){step++;since=System.currentTimeMillis();}
    private void capture(String name){ScreenShotHelper.saveScreenshot(mc.mcDataDir,name,mc.displayWidth,mc.displayHeight,mc.getFramebuffer());System.out.println("OPAI_CLIENT SCREENSHOT "+new File(mc.mcDataDir,"screenshots/"+name));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void tick(TickEvent.RenderTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        long elapsed=System.currentTimeMillis()-since;
        try{
            if(step==0 && mc.currentScreen instanceof GuiMainMenu && elapsed>1000){
                SettingsModule.customCape.setValue(0);SettingsModule.cape=SettingsModule.Cape.None;
                require(SettingsModule.opaiVisuals!=null && SettingsModule.opaiColor!=null,"Settings owns Opai visuals and Opai color");
                SettingsModule.opaiVisuals.set(true);SettingsModule.opaiColor.setValue(1);ClientConfig.save();
                SettingsModule.opaiVisuals.set(false);SettingsModule.opaiColor.setValue(0);ClientConfig.load();
                require(OpaiRuntime.enabled() && OpaiRuntime.palette()==OpaiStyle.LIGHT_PINK,"Settings persists enabled mode and palette");
                require(OpaiRuntime.ready(),"Native NanoVG initializes in Minecraft LWJGL2 context");
                mc.launchIntegratedServer("opai-verification","Opai verification",new WorldSettings(0,WorldSettings.GameType.CREATIVE,false,false,WorldType.FLAT));advance();
            }else if(step==1 && mc.thePlayer!=null && elapsed>1500){
                ModuleManager.get("HUD").enable();ModuleManager.get("Sprint").enable();
                SettingsModule.opaiColor.setValue(0);mc.displayGuiScreen(ClickGui.instance);ClickGui.instance.initMain();
                java.lang.reflect.Field field=ClickGui.class.getDeclaredField("opai");field.setAccessible(true);OpaiScreen screen=(OpaiScreen)field.get(ClickGui.instance);
                field=OpaiScreen.class.getDeclaredField("ui");field.setAccessible(true);ui=(OpaiUi)field.get(screen);
                java.lang.reflect.Method show=OpaiUi.class.getDeclaredMethod("showSettings");show.setAccessible(true);show.invoke(ui);
                require(ui.columns.stream().flatMap(c->c.features.stream()).anyMatch(f->f.name().equals("Settings") && f.controls().stream().anyMatch(c->c.name().equals("Opai visuals"))),"Real B4 Settings adapter exposes Opai switch");advance();
            }else if(step==2 && elapsed>1200){
                OpaiUi.Column settings=ui.columns.stream().filter(c->c.name.equals("Misc")).findFirst().get();
                require(settings.x>=0 && settings.x+settings.w<=new net.minecraft.client.gui.ScaledResolution(mc).getScaledWidth(),"Settings shortcut reveals column in narrow window");
                capture("opai-minecraft-settings.png");mc.displayGuiScreen(new OpaiHudEditor());advance();
            }else if(step==3 && elapsed>1200){
                require(OpaiRuntime.boxes().stream().anyMatch(b->b.name.equals("ArrayList")),"Production ArrayList renders");
                require(OpaiRuntime.boxes().stream().anyMatch(b->b.name.equals("Dynamic Island")),"Production Dynamic Island renders");
                require(OpaiRuntime.boxes().stream().anyMatch(b->b.name.equals("Target HUD")),"Production Target HUD renders");
                capture("opai-minecraft-hud.png");OpaiRuntime.success("Visuals ready","All four surfaces share Settings");advance();
            }else if(step==4 && elapsed>400){capture("opai-minecraft-notice.png");SettingsModule.opaiVisuals.set(false);SettingsModule.opaiVisuals.set(true);OpaiRuntime.styleChanged();SettingsModule.opaiVisuals.set(false);mc.displayGuiScreen(ClickGui.instance);ClickGui.instance.initMain();advance();
            }else if(step==5 && elapsed>1000){
                capture("raven-minecraft-fallback.png");require(!OpaiRuntime.enabled(),"Settings disabled mode returns to Raven");finish(true);
            }
            if(elapsed>90000)throw new AssertionError("Integration check timed out at step "+step);
        }catch(Throwable error){error.printStackTrace();evidence.append("FAIL ").append(error).append('\n');finish(false);}
    }
    private void finish(boolean success){
        MinecraftForge.EVENT_BUS.unregister(this);
        try{Files.write(new File(mc.mcDataDir,"opai-client-result.txt").toPath(),(evidence.toString()+"RESULT "+(success?"PASS":"FAIL")+"\n").getBytes(StandardCharsets.UTF_8));}catch(Exception error){error.printStackTrace();}
        mc.loadWorld(null);mc.shutdown();
    }
}
