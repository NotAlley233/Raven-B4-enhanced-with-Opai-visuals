package xyz.daikirai.gui.opai;

import java.util.List;

/** Exact Opai width, baseline, edge and stepped corner geometry, independent of game version. */
public final class OpaiArraylistPainter {
    public static final class Row {
        public final String name,suffix;
        public final float x,y,width,nameWidth,gap;
        public Row(String name,String suffix,float x,float y,float width,float nameWidth,float gap){this.name=name;this.suffix=suffix;this.x=x;this.y=y;this.width=width;this.nameWidth=nameWidth;this.gap=gap;}
    }
    public static float measure(OpaiCanvas c,String text){return c.measure(text,9.08f,"googlesans-bold",0);}
    public static float width(OpaiCanvas c,String name,String suffix){return 6+measure(c,name)+(suffix.isEmpty()?0:measure(c," ")+measure(c,suffix));}
    public static void paint(OpaiCanvas c,OpaiStyle.Palette p,List<Row> rows,boolean background,boolean shadow,boolean edge){
        if(shadow)for(int i=0;i<rows.size();i++){Row row=rows.get(i);float next=i+1<rows.size()?rows.get(i+1).width:0,radius=Math.min(5,Math.max(0,row.width-next));c.shadow(row.x,row.y,row.width,12,radius);}
        if(background){
            c.path();
            for(int i=0;i<rows.size();i++){Row row=rows.get(i);float next=i+1<rows.size()?rows.get(i+1).width:0,radius=Math.min(5,Math.max(0,row.width-next));c.roundedPath(row.x,row.y,row.width,12,0,0,0,radius);}
            c.fill(0xCC101010);
        }
        for(Row row:rows){
            c.baselineText(row.name,row.x+3,row.y+8.5f,9.08f,"googlesans-bold",0xFFFFFFFF);
            if(!row.suffix.isEmpty())c.baselineText(row.suffix,row.x+3+row.nameWidth+row.gap,row.y+8.5f,9.08f,"googlesans-bold",p.accent());
        }
        if(edge){c.path();for(Row row:rows)c.roundedPath(row.x+row.width-1,row.y,1,12,0,0,0,0);c.fill(p.accent());}
    }
}
