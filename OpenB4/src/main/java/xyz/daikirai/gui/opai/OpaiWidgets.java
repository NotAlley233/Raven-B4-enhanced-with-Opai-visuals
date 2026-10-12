package xyz.daikirai.gui.opai;

import static xyz.daikirai.gui.opai.OpaiMath.*;

/** The reference's actual control geometry, shared by game rendering and visual verification. */
public final class OpaiWidgets {
    private OpaiWidgets() { }
    public static void toggle(OpaiCanvas c, OpaiStyle.Palette p, float x, float y, float t, float press) {
        c.rounded(x,y,26,16,8,mix(p.toggleOutline(),p.accent(),t));
        if(t<1) c.rounded(x+1,y+1,24,14,7,alpha(p.field(),1-t));
        float stretch=3*press, knob=x+2+(10-stretch)*t;
        c.rounded(knob,y+2,12+stretch,12,6,mix(p.fieldLine(),p.enabledText(),t));
        if(t>.01f) c.check(knob+3,y+5,6,alpha(p.accent(),t));
    }
    public static void slider(OpaiCanvas c,OpaiStyle.Palette p,float x,float y,float w,float fraction,float pulse) {
        float track=w-20, cy=y+15.6f, knob=x+10+track*fraction;
        c.rounded(x+10,cy-1.25f,track,2.5f,1.25f,p.track());
        c.rounded(x+10,cy-1.25f,track*fraction,2.5f,1.25f,p.accent());
        if(pulse>0) c.rounded(knob-7,cy-7,14,14,7,alpha(p.accent(),.25f*pulse));
        c.rounded(knob-4.5f,cy-3.5f,9,9,4.5f,0x33000000);
        c.rounded(knob-4,cy-4,8,8,4,p.accent());
    }
    public static void field(OpaiCanvas c,OpaiStyle.Palette p,float x,float y,float w,String value,boolean open) {
        c.rounded(x+8,y+16,w-16,21,2,p.field());
        if(open) c.rect(x+8,y+19,w-16,21,p.field());
        c.text(value,x+14,y+26.5f,8.5f,w-28,p.text());
        c.rect(x+9,y+36,w-18,1,open?p.enabled():p.fieldLine());
    }
    public static void pencil(OpaiCanvas c,float x,float y,int accent) {
        c.rounded(x,y,26.7f,26.7f,5,accent); c.save();
        try {
            c.translate(x,y); c.path(); c.move(6.5f,16.4f); c.lineTo(15.7f,7.2f); c.lineTo(19.5f,11);
            c.lineTo(10.3f,20.2f); c.lineTo(6.2f,20.5f); c.closePath();
            c.move(16.6f,6.3f); c.lineTo(17.4f,5.5f); c.bezier(20.1f,2.8f,23.9f,6.6f,21.2f,9.3f);
            c.lineTo(20.4f,10.1f); c.closePath(); c.fill(0xFF101014);
            c.rounded(6,22,16,1.6f,.5f,0xFF101014);
        } finally { c.restore(); }
    }
}
