package util;

import gui.View;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import modules.BaseModule;
import modules.Link;
import modules.parts.Input;
import modules.parts.Output;
import modules.parts.Port;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import simulator.Main;

public class XMLWriter {
    
    /**
     * Generates unique IDs for entities in the simulation
     */
    private static void genIDs() {
        synchronized (Main.sim) {
            int id = 0;
            
            // Loop the modules and their ports
            for (BaseModule m : Main.sim.getModules()) {
                m.ID = id++;
                
                for (Port p : m.ports) {
                    p.ID = id++;
                }
            }
        }
    }

    /**
     * Write an XML format file
     *
     * @param path
     */
    public static void writeFile(File xmlFile) {
        try {
            DocumentBuilderFactory dbF = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbF.newDocumentBuilder();

            Document doc = db.newDocument();

            // Build the document... add elements, attributes to represent the entities
            Element rootElem = doc.createElement("ModuleSim");
            doc.appendChild(rootElem);

            // Store the view information
            Element view = doc.createElement("view");
            View v = Main.ui.view;
            view.setAttribute("camX", "" + v.camX);
            view.setAttribute("camY", "" + v.camY);
            view.setAttribute("zoom", "" + v.zoomI);
            rootElem.appendChild(view);

            synchronized (Main.sim) {
                // Generate IDs for storage
                genIDs();
                
                // Store the modules
                List<BaseModule> modules = Main.sim.getModules();
                Element mods = doc.createElement("modules");
                rootElem.appendChild(mods);

                for (BaseModule m : modules) {
                    Element modElem = doc.createElement("module");
                    modElem.setAttribute("ID", "" + m.getID());
                    modElem.setAttribute("type", m.getModType().name());

                    // Dimensions
                    Element dim = doc.createElement("dim");
                    dim.setAttribute("x", "" + m.pos.x);
                    dim.setAttribute("y", "" + m.pos.y);
                    dim.setAttribute("orient", "" + m.orientation);
                    modElem.appendChild(dim);

                    // Inputs
                    Element inputs = doc.createElement("inputs");

                    for (Input i : m.inputs) {
                        Element e = doc.createElement("input");
                        e.setAttribute("ID", "" + i.getID());
                        inputs.appendChild(e);
                    }

                    modElem.appendChild(inputs);

                    // Outputs
                    Element outputs = doc.createElement("outputs");

                    for (Output o : m.outputs) {
                        Element e = doc.createElement("output");
                        e.setAttribute("ID", "" + o.getID());
                        outputs.appendChild(e);
                    }

                    modElem.appendChild(outputs);

                    // Data - stored only if the module's dataOut override indicates a modification has been made
                    Element data = doc.createElement("data");
                    if (m.dataOut(data)) {
                        modElem.appendChild(data);
                    }

                    mods.appendChild(modElem);
                }

                // Store the links
                List<Link> links = Main.sim.getLinks();
                Element linksElem = doc.createElement("links");
                rootElem.appendChild(linksElem);

                for (Link l : links) {
                    Element lElem = doc.createElement("link");
                    lElem.setAttribute("src", "" + l.src.ID);
                    lElem.setAttribute("targ", "" + l.targ.ID);

                    // Curve points
                    for (CtrlPt c : l.curve.ctrlPts) {
                        Element point = doc.createElement("ctrlPt");
                        point.setAttribute("x", "" + c.pos.x);
                        point.setAttribute("y", "" + c.pos.y);
                        lElem.appendChild(point);
                    }

                    linksElem.appendChild(lElem);
                }
            }

            // Store the latest id
            /*Element idElem = doc.createElement("lastid");
            idElem.setAttribute("id", "" + Main.lastID);
            rootElem.appendChild(idElem);*/

            // Saving operation
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            DOMSource src = new DOMSource(doc);
            StreamResult r = new StreamResult(xmlFile);

            t.transform(src, r);
            System.out.println("Saved simulation to " + xmlFile.getAbsolutePath());

            Main.sim.filePath = xmlFile.getPath();
            Main.ui.updateTitle();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}