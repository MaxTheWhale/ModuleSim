package com.modsim.tools;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import com.modsim.Main;
import com.modsim.operations.CreateOperation;
import com.modsim.res.Colors;
import com.modsim.util.*;
import com.modsim.gui.view.ViewUtil;
import com.modsim.modules.*;
import com.modsim.modules.parts.Port;

/**
 * Module linking tool
 * @author aw12700
 *
 */
public class MakeLinkTool extends BaseTool {

	public Port source;

	public Vec2 start = new Vec2();
	public Vec2 current = new Vec2();
	public BezierPath curve;

	public boolean working = false;

	@Override
	public BaseTool lbDown(int x, int y) {
		// Get the clicked port, start linking from here
		if (!working) {
			if (!startLink(x, y)) {
				return null;
			}
		}
		// End / place new control point
		else {
			if (endLink(x, y)) {
				return null;
			}
		}

		return this;
	}

	@Override
	public BaseTool mouseMove(int x, int y) {
		if (working) {
			current = ViewUtil.screenToWorld(new Vec2(x, y));
			Vec2 tmp = new Vec2(current);
				if (Main.ui.view.snapToGrid)
				{
					tmp.x = roundToGrid(tmp.x);
					tmp.y = roundToGrid(tmp.y);
				}
			Port p = ViewUtil.screenSpace_portAt(tmp.x, tmp.y);
			if (p != null) {
				curve.setEnd(p);
			}
			else {	
				curve.setEnd(tmp);
			}
		}

		return this;
	}
	@Override
	public BaseTool mouseDrag(int x, int y) {
		return mouseMove(x, y);
	}

	@Override
	public BaseTool keyDown(int key) {
		if (key == KeyEvent.VK_BACK_SPACE) {
			if (curve.removePt()) 	return this;
			else 					return null;
		}

		return this;
	}

	@Override
	public void paintWorld(Graphics2D g) {
		if (working) {
			g.setColor(Colors.selectedLink);
			g.setStroke(new BasicStroke(2));
			curve.draw(g);
		}
	}

	/**
	 * Attempts to start a new link
	 * @param x X position to check
	 * @param y Y position to check
	 * @return True if successful
	 */
	public boolean startLink(int x, int y) {
		source = ViewUtil.screenSpace_portAt(x, y);
		if (source != null) {
			working = true;
			start = new Vec2(x, y);
			curve = new BezierPath();

			curve.setEnd(source.getDisplayPosW());
			curve.setStart(source);
			if (Main.ui.view.useStraightLines) curve.addPt(new CtrlPt(source.getDisplayPosW()));
			return true;
		}

		return false;
	}

	/**
	 * Attempts to complete the link. Adds a control point if unsuccessful.
	 * @param x X position to check
	 * @param y Y position to check
	 * @return True if link was completed
	 */
	public boolean endLink(int x, int y) {
		Port targ = ViewUtil.screenSpace_portAt(x, y);

		if (targ != null) {
			curve.addPt(new CtrlPt(targ.getDisplayPosW()));
			Main.opStack.beginCompoundOp();
			Link l = Link.createLink(source, targ, curve);
			if (l != null) {
	            Main.sim.addLink(l);
	            Main.sim.propagate(l.targ.owner);
				Main.opStack.pushOp(new CreateOperation(l));
			}
			Main.opStack.endCompoundOp();
			working = false;
			return true;
		}
		else {
			Vec2 tmp = new Vec2(current);
			if (Main.ui.view.snapToGrid)
			{
				tmp.x = roundToGrid(tmp.x);
				tmp.y = roundToGrid(tmp.y);
			}
			curve.addPt(new CtrlPt(tmp));
			if (Main.ui.view.useStraightLines) curve.addPt(new CtrlPt(tmp));
			return false;
		}
	}

	private static double roundToGrid(double x) {
		int gSize = Main.sim.grid;
		float gSizeHalf = gSize / 2.0f;
		return (x > 0) ? (x + gSizeHalf) - ((x + gSizeHalf) % gSize) : (x - gSizeHalf) - ((x - gSizeHalf) % -gSize);
	}

	@Override
	public void cancel() {

	}
}
