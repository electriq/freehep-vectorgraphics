// Copyright 2007 FreeHEP
// Modified by The MathWorks (www.mathworks.com), Oct. 2010
package org.freehep.graphicsio.ps;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.freehep.graphicsio.PageConstants;

/**
 * Writes Encapsulated PostScript with bounding box.
 * @author duns
 * @author Peter Webb (for The MathWorks)
 * @version $Id: freehep-graphicsio-ps/src/main/java/org/freehep/graphicsio/ps/EPSGraphics2D.java bed5e3a39f35 2007/09/10 18:13:00 duns $
 */
public class EPSGraphics2D extends AbstractPSGraphics2D {

    public EPSGraphics2D(File file, Dimension size) throws FileNotFoundException {
        this(new FileOutputStream(file), size);
    }

    public EPSGraphics2D(File file, Component component)
            throws FileNotFoundException {
        this(new FileOutputStream(file), component);
    }

    public EPSGraphics2D(OutputStream os, Dimension size) {
        super(size, false);
        init(os);
    }

    public EPSGraphics2D(OutputStream os, Component component) {
        super(component, false);
        init(os);
    }
    // Strictly speaking, EPS graphics should always have origin (0,0), since they
    // are meant to be embedded in a page, their position controlled by the containing
    // document.
    public EPSGraphics2D(OutputStream ros, Rectangle bbox) {
		super( new Rectangle(0, 0, bbox.width, bbox.height), false);
        init(ros);
    }

    public EPSGraphics2D(OutputStream ros, Rectangle pos, int psLangLevel) {
		super(pos, false, psLangLevel);
        init(ros);
    }
    protected EPSGraphics2D(EPSGraphics2D graphics, boolean doRestoreOnDispose) {
    	super(graphics, doRestoreOnDispose);
    }

    /**
     * Get the bounding box for this page.
     *
     * @return pageSize - margins
     */
    private Rectangle getBoundingBox() {
        // determine page size
        Dimension pageSize = getPageSize();

        // Our PS Header has internal page orientation mode, all sizes given in
        // portrait
        Insets margins = getPropertyInsets(PAGE_MARGINS);
        boolean isPortrait = getProperty(ORIENTATION).equals(
                PageConstants.PORTRAIT);

        // Available width and height.
        double awidth = (pageSize.width - margins.left - margins.right);
        double aheight = (pageSize.height - margins.top - margins.bottom);

        // Image width and height (adjusted for portrait or landscape)
        Dimension size = getSize();
        double iwidth = (isPortrait ? size.width : size.height);
        double iheight = (isPortrait ? size.height : size.width);

        // Choose the minimum scale factor.
        double sf = Math.min(awidth / iwidth, aheight / iheight);
        if (!isProperty(FIT_TO_PAGE)) {
            sf = Math.min(sf, 1.);
        }

        // Lower left corner.
        double x0 = awidth / 2. + margins.left - sf * iwidth / 2.;
        double y0 = aheight / 2. + margins.bottom - sf * iheight / 2.;

        // Upper right corner.
        double x1 = x0 + sf * iwidth;
        double y1 = y0 + sf * iheight;

        int llx = (int) x0;
        int lly = (int) y0;
        int urx = (int) Math.ceil(x1);
        int ury = (int) Math.ceil(y1);

        // Convert these back to integer values.
        return new Rectangle(llx, lly, urx - llx, ury - lly);
    }

    /**
     *
     * @return EPS-compliant bounding box, with lower left corner at 0,0
     */
    public Rectangle getEPSBoundingBox() {
         // Image width and height (adjusted for portrait or landscape)
        boolean isPortrait = getProperty(ORIENTATION).equals(PageConstants.PORTRAIT);
        Dimension size = getSize();
        double iwidth = (isPortrait ? size.width : size.height);
        double iheight = (isPortrait ? size.height : size.width);
        return new Rectangle(0, 0, (int)Math.ceil(iwidth), (int)Math.ceil(iheight));
    }

    public void writeHeader() throws IOException {
        Dimension size = getSize();
        // moved to openPage for multiPage
        resetClip(new Rectangle(0, 0, size.width, size.height));

        os = new PrintStream(ros, true);
        os.println("%!PS-Adobe-3.0" + " EPSF-3.0");
        Rectangle bbox = getEPSBoundingBox();
        os.println("%%BoundingBox: " + bbox.x + " " + bbox.y + " "
                    + (bbox.x + bbox.width) + " " + (bbox.y + bbox.height));

        super.writeHeader();

        os.println("%%BeginSetup");

        super.openPage(getSize(), null, getComponent());
        
        os.println("%%EndSetup");
        os.println();

        try {
            writeGraphicsState();
            writeBackground();
        } catch (Exception e) {
            handleException(e);
        }
    }

    // EPS files should not call generally call setmatrix. And if they do, they must set the
    // matrix to something derived from currentmatrix. But this is exactly what concatmatrix
    // does, so call it instead. (EPS files do not control the position of their graphics
    // on the page.)
    //
    // AbstractVectorGraphicsIO.writeGraphicsState calls setmatrix (via writeSetTransform).
    // Here instead we call writeTransform, which concatenates the EPS transformation matrix
    // onto the global document matrix.
    public void writeGraphicsState() throws IOException {
		writePaint(getPrintColor(getColor()));

		writeTransform(getTransform());
        setClip(getClip());
    }
    
    public void writeTrailer() throws IOException {
        super.writeTrailer();
        os.println("%%EOF");
    }    
    
    public Graphics create() {
        try {
            writeGraphicsSave();
        } catch (IOException e) {
            handleException(e);
        }
        return new EPSGraphics2D(this, true);
    }

    public Graphics create(double x, double y, double width, double height) {
        try {
            writeGraphicsSave();
        } catch (IOException e) {
            handleException(e);
        }
        EPSGraphics2D graphics = new EPSGraphics2D(this, true);
        graphics.translate(x, y);
        graphics.clipRect(0, 0, width, height);
        return graphics;
    }    
}
