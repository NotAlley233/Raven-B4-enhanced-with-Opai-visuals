package xyz.daikirai.gui.opai;

/** Samsara's analytical critically damped array-list response (GPL-3.0). */
final class OpaiSpring {
    private final double rate;
    private final boolean immediateDeparture;
    private double value,velocity,target;
    private long last=-1;
    OpaiSpring(double value,double rate){this(value,rate,false);}
    OpaiSpring(double value,double rate,boolean immediateDeparture){this.value=this.target=value;this.rate=rate;this.immediateDeparture=immediateDeparture;}
    double to(double target,long now){
        if(last>=0 && now>last){double dt=(now-last)/1000.0,offset=value-this.target,linear=velocity+rate*offset,decay=Math.exp(-rate*dt);value=this.target+(offset+linear*dt)*decay;velocity=(velocity-rate*linear*dt)*decay;}
        if(target!=this.target && immediateDeparture && Math.abs(value-this.target)<.002 && Math.abs(velocity)<.02)velocity=rate*(target-value);
        last=now;this.target=target;return value;
    }
    double value(){return value;}
    void snap(double value,long now){this.value=this.target=value;velocity=0;last=now;}
}
