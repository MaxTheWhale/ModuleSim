package com.modsim.modules;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import com.modsim.modules.ports.BidirPort;
import com.modsim.Main;
import com.modsim.modules.parts.Port;
import com.modsim.modules.parts.SSText;
import com.modsim.res.Colors;
import com.modsim.simulator.PickableEntity;
import com.modsim.util.BezierCurve;
import com.modsim.util.BinData;
import com.modsim.util.Vec2;

public class SplitMerge extends BaseModule {

    private final BidirPort portA0, portA1;
    private final BidirPort portB0, portB1, portB2, portB3;

    private final List<BezierCurve> curves;

    public SplitMerge() {
        w = 150;
        h = 50;

        // Add bi-directional inputs/outputs
        portA0 = addBidirInput("4-Bit Port A0", -25, Port.GENERIC);
        portA1 = addBidirInput("2-Bit Port A1", 25, Port.GENERIC);

        portB0 = addBidirOutput("Port B0", -50, Port.GENERIC);
        portB1 = addBidirOutput("Port B1", -25, Port.GENERIC);
        portB2 = addBidirOutput("Port B2",  25, Port.GENERIC);
        portB3 = addBidirOutput("Port B3",  50, Port.GENERIC);

        // Curves - basically a visual wiring guide
        BezierCurve[] cs = new BezierCurve[9];

        // offsets
        int A, B, a, b, c, d, b0, b1, b2, b3;
        A = -25; B = 25;
        a = -50; b = -25; c = 25; d = 50;
        b0 = 6; b1 = 2; b2 = -2; b3 = -6;

        // curves to draw
        cs[0] = new BezierCurve(new Vec2(A + b0,  23),  new Vec2(a - b0, -23), // A0-a0
                                new Vec2(A + b0, -17),  new Vec2(a - b0, 13));

        cs[1] = new BezierCurve(new Vec2(A + b1,  23),  new Vec2(b - b0, -23), // A1-b0
                                new Vec2(A + b1, -15),  new Vec2(b - b0, 15));

        cs[2] = new BezierCurve(new Vec2(A + b1,  23),  new Vec2(a - b1, -23), // A1-a1
                                new Vec2(A + b1, -13),  new Vec2(a - b1, 17));

        cs[3] = new BezierCurve(new Vec2(A + b2,  23),  new Vec2(c - b0, -23), // A2-c0
                                new Vec2(A + b2, -15),  new Vec2(c - b0, 15));

        cs[4] = new BezierCurve(new Vec2(A + b3,  23),  new Vec2(d - b0, -23), // A3-d0
                                new Vec2(A + b3, -20),  new Vec2(d - b0, 5));

        cs[5] = new BezierCurve(new Vec2(A + b3,  23),  new Vec2(c - b1, -23), // A3-c1
                                new Vec2(A + b3, -17),  new Vec2(c - b1, 8));


        cs[6] = new BezierCurve(new Vec2(B + b0,  23),  new Vec2(c - b0, -23), // B0-c0
                                new Vec2(B + b0, -15),  new Vec2(c - b0, 15));

        cs[7] = new BezierCurve(new Vec2(B + b1,  23),  new Vec2(c - b1, -23), // B1-c1
                                new Vec2(B + b1, -15),  new Vec2(c - b1, 15));

        cs[8] = new BezierCurve(new Vec2(B + b1,  23),  new Vec2(d - b0, -23), // B1-d0
                                new Vec2(B + b1, -15),  new Vec2(d - b0, 15));
        curves = Collections.unmodifiableList(Arrays.asList(cs));
        
        parts.add(new SSText(A-10, 24, "DCBA", 7, Colors.labelText));
        parts.add(new SSText(B-10, 24, "XXDC", 7, Colors.labelText));
        parts.add(new SSText(a+10, -24, "ABXX", -7, Colors.labelText));
        parts.add(new SSText(b+10, -24, "BXXX", -7, Colors.labelText));
        parts.add(new SSText(c+10, -24, "CDXX", -7, Colors.labelText));
        parts.add(new SSText(d+10, -24, "DXXX", -7, Colors.labelText));

    }

    @Override
    public void paintStatic(Graphics2D g) {
        // Fill in polygon
        g.setColor(Colors.moduleFill);
        drawBox(g, 10);

        // Show drawing
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < curves.size(); i++) {
            int grad = 0;
            switch (i) {
            case 0:
                grad = 120;
                break;
            case 1:
            case 2:
                grad = 140;
                break;
            case 3:
                grad = 160;
                break;
            case 4:
            case 5:
                grad = 180;
                break;
            case 6:
                grad = 200;
                break;
            case 7:
            case 8:
                grad = 220;
                break;
            }
            g.setColor(new Color(grad, grad, grad));
            curves.get(i).draw(g);
        }

        // Show output/input
        g.setColor(Colors.modulePorts);
        drawBidir(g);

        drawStaticParts(g);
    }

    @Override
    public void propagate() {
        // Connections
        BinData a0_val, a1_val, b0_val, b1_val, b2_val, b3_val;
        a0_val = portA0.getVal();
        a1_val = portA1.getVal();
        b0_val = portB0.getVal();
        b1_val = portB1.getVal();
        b2_val = portB2.getVal();
        b3_val = portB3.getVal();

        // Switch based on propagation direction
        if (portA0.wasUpdated() || portA1.wasUpdated()) {
        	if(portA0.isConnected() && portA1.isConnected())
        	{
        		JOptionPane.showMessageDialog(Main.ui.pane, "Error: There must only be one connection to that size of split/merge.");
        		Port port = portA0.wasUpdated()?portA0:portA1;
        		synchronized (Main.sim)
				{
	        		Main.sim.removeLink(port.link);	        		
        		}
        		return;
        	}
            b0_val.setBit(0, a0_val.getBit(0)); // A0-a0
            b1_val.setBit(0, a0_val.getBit(1)); // A1-b1
            b0_val.setBit(1, a0_val.getBit(1)); // A1-a1

            // Resolution of 3-state logic for merges
            b3_val.setBit(0, a0_val.getBit(3));   // A3-d0
            b3_val.resolveBit(0, a1_val.getBit(1)); // B1-d0

            b2_val.setBit(0, a0_val.getBit(2));     // A2-c0
            b2_val.resolveBit(0, a1_val.getBit(0)); // B0-c0

            b2_val.setBit(1, a0_val.getBit(3));     // A3-c1
            b2_val.resolveBit(1, a1_val.getBit(1)); // B1-c1
        }
        else if (   portB0.wasUpdated() || portB1.wasUpdated() ||
                    portB2.wasUpdated() || portB3.wasUpdated()) {
            a0_val.setBit(0, b0_val.getBit(0)); // a0-A0
            a0_val.setBit(2, b2_val.getBit(0)); // c0-A2
            a1_val.setBit(0, b2_val.getBit(0)); // c0-B0

            // Resolution of 3-state logic for merges
            int val = b2_val.getBit(1) | b3_val.getBit(0);
            a1_val.setBit(1, val);
            a0_val.setBit(3, val);
            
            val = b0_val.getBit(1) | b1_val.getBit(0);
            a0_val.setBit(1, val);
        }

        // Set the values
        portA0.setVal(a0_val);
        portA1.setVal(a1_val);

        portB0.setVal(b0_val);
        portB1.setVal(b1_val);
        portB2.setVal(b2_val);
        portB3.setVal(b3_val);
    }

    @Override
    public List<Port> getAffected(Port in) {
        List<Port> outPorts = new ArrayList<>();

        // a0->b0, b1, b2, b3
        // a1->b2, b3
        // b0->a0
        // b1->a0
        // b2->a0, a1
        // b3->a0, a1

        /*if (in == portA0 || in == portA1) {
            outPorts.add(portB2);
            outPorts.add(portB3);

            if (in == portA0) {
                outPorts.add(portB0);
                outPorts.add(portB1);
            }
        }

        if (in == portB0 || in == portB1 || in == portB2 || in == portB3) {
            outPorts.add(portA0);

            if (in == portB2 || in == portB3) {
                outPorts.add(portA1);
            }
        }

        return outPorts;*/

        // The above code creates problems with possible loops through multiple split/mergers (the direction
        // propagation never resolves).

        // Much simpler to just pick out ports on the opposite side.
        if (in == portA0 || in == portA1) {
            outPorts.add(portB0);
            outPorts.add(portB1);
            outPorts.add(portB2);
            outPorts.add(portB3);
        }
        else {
            outPorts.add(portA0);
            outPorts.add(portA1);
        }

        return outPorts;
    }

    @Override
    public PickableEntity createNew() {
        return new SplitMerge();
    }

    @Override
    public AvailableModules getModType() {
        return AvailableModules.SPLIT_MERGE;
    }

}