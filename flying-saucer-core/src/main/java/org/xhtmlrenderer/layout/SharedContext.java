/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.layout;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.context.StyleReference;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.EmptyStyle;
import org.xhtmlrenderer.extend.FSCanvas;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.RenderingContext;
import com.github.neoflyingsaucer.extend.output.FSFont;
import com.github.neoflyingsaucer.extend.output.FSFontMetrics;
import com.github.neoflyingsaucer.extend.output.FSImage;
import com.github.neoflyingsaucer.extend.output.FontContext;
import com.github.neoflyingsaucer.extend.output.FontResolver;
import com.github.neoflyingsaucer.extend.output.FontSpecificationI;
import com.github.neoflyingsaucer.extend.output.ImageResolver;
import com.github.neoflyingsaucer.extend.output.ReplacedElementResolver;
import com.github.neoflyingsaucer.extend.useragent.ImageResourceI;
import com.github.neoflyingsaucer.extend.useragent.Optional;
import com.github.neoflyingsaucer.extend.useragent.UserAgentCallback;

/**
 * The SharedContext is that which is kept between successive
 * layout and render runs.
 *
 * @author empty
 */
public class SharedContext {

    private TextRenderer text_renderer;
    private String media;
    private UserAgentCallback uac;
    private Map<String, Box> idMap;

    private StylesheetInfo defaultStylesheet;
    private boolean lookedUpDefaultStylesheet;
    private Locale localeTextBreaker = Locale.US;
    private ImageResolver imgResolver;
    private NamespaceHandler namespaceHandler;
	private ReplacedElementResolver replacedElementResolver;
    
    private String _uri;
    /*
     * used to adjust fonts, ems, points, into screen resolution
     */
    /**
     * Description of the Field
     */
    private float dpi;
    /**
     * Description of the Field
     */
    private final static int MM__PER__CM = 10;
    /**
     * Description of the Field
     */
    private final static float CM__PER__IN = 2.54F;
    /**
     * dpi in a more usable way
     */
    private float mm_per_dot;

    private final static float DEFAULT_DPI = 72;
    private boolean print;

    private int dotsPerPixel = 1;

    private Map<Element, CalculatedStyle> styleMap;

    private ReplacedElementFactory replacedElementFactory;
    private Rectangle temp_canvas;
    
    private Dimension _deviceDimension;

    public SharedContext()
    {
    }

    /**
     * Constructor for the Context object
     */
    public SharedContext(final UserAgentCallback uac) {
    	this();
    	
    	//font_resolver = new AWTFontResolver();
        //replacedElementFactory = new SwingReplacedElementFactory();
        setMedia("screen");
        this.uac = uac;
        setCss(new StyleReference(uac));
        //setTextRenderer(new Java2DTextRenderer());
        try {
            setDPI(Toolkit.getDefaultToolkit().getScreenResolution());
        } catch (final HeadlessException e) {
            setDPI(DEFAULT_DPI);
        }
    }


    /**
     * Constructor for the Context object
     */
    public SharedContext(final UserAgentCallback uac, final FontResolver fr, final ReplacedElementFactory ref, final TextRenderer tr, final float dpi) {
    	this();
    	
    	font_resolver = fr;
        replacedElementFactory = ref;
        setMedia("screen");
        this.uac = uac;
        setCss(new StyleReference(uac));
        setTextRenderer(tr);
        setDPI(dpi);
    }

    public LayoutContext newLayoutContextInstance() {
        final LayoutContext c = new LayoutContext(this);
        return c;
    }

    public RenderingContext newRenderingContextInstance() {
        final RenderingContext c = new RenderingContext(this);
        return c;
    }

    /*
=========== Font stuff ============== */

    /**
     * Gets the fontResolver attribute of the Context object
     *
     * @return The fontResolver value
     */
    public FontResolver getFontResolver() {
        return font_resolver;
    }

    public void flushFonts() {
        font_resolver.flushCache();
    }

    /**
     * Description of the Field
     */
    protected FontResolver font_resolver;

    /**
     * The media for this context
     */
    public String getMedia() {
        return media;
    }

    /**
     * Description of the Field
     */
    protected StyleReference css;

    /**
     * Description of the Field
     */
    protected boolean debug_draw_boxes;

    /**
     * Description of the Field
     */
    protected boolean debug_draw_line_boxes;
    protected boolean debug_draw_inline_boxes;
    protected boolean debug_draw_font_metrics;

    /**
     * Description of the Field
     */
    protected FSCanvas canvas;

    /*
     * selection management code
     */
    /**
     * Description of the Field
     */
    protected Box selection_start, selection_end;

    /**
     * Description of the Field
     */
    protected int selection_end_x, selection_start_x;


    /**
     * Description of the Field
     */
    protected boolean in_selection = false;

    public TextRenderer getTextRenderer() {
        return text_renderer;
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawBoxes() {
        return debug_draw_boxes;
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawLineBoxes() {
        return debug_draw_line_boxes;
    }

    /**
     * Description of the Method
     *
     * @return Returns
     */
    public boolean debugDrawInlineBoxes() {
        return debug_draw_inline_boxes;
    }

    public boolean debugDrawFontMetrics() {
        return debug_draw_font_metrics;
    }

    public void setDebug_draw_boxes(final boolean debug_draw_boxes) {
        this.debug_draw_boxes = debug_draw_boxes;
    }

    public void setDebug_draw_line_boxes(final boolean debug_draw_line_boxes) {
        this.debug_draw_line_boxes = debug_draw_line_boxes;
    }

    public void setDebug_draw_inline_boxes(final boolean debug_draw_inline_boxes) {
        this.debug_draw_inline_boxes = debug_draw_inline_boxes;
    }

    public void setDebug_draw_font_metrics(final boolean debug_draw_font_metrics) {
        this.debug_draw_font_metrics = debug_draw_font_metrics;
    }


    /*
=========== Selection Management ============== */


    public StyleReference getCss() {
        return css;
    }

    public void setCss(final StyleReference css) {
        this.css = css;
    }

    public FSCanvas getCanvas() {
        return canvas;
    }

    public void setCanvas(final FSCanvas canvas) {
        this.canvas = canvas;
    }

    public void set_TempCanvas(final Rectangle rect) {
        this.temp_canvas = rect;
    }

    public void setDeviceDimension(Dimension dim)
    {
    	_deviceDimension = dim;
    }
    
    public Dimension getDeviceDimension()
    {
    	if (_deviceDimension == null)
    		return new Dimension(getFixedRectangle().width, getFixedRectangle().height);
    	
    	return _deviceDimension;
    }

    public Rectangle getFixedRectangle() {
        //Uu.p("this = " + canvas);
        if (getCanvas() == null) {
            return this.temp_canvas;
        } else {
            final Rectangle rect = getCanvas().getFixedRectangle();
            rect.translate(getCanvas().getX(), getCanvas().getY());
            return rect;
        }
    }

    public void setNamespaceHandler(final NamespaceHandler nh) {
        namespaceHandler = nh;
    }

    public NamespaceHandler getNamespaceHandler() {
        return namespaceHandler;
    }

    public void addBoxId(final String id, final Box box) {
        if (idMap == null) {
            idMap = new HashMap<String, Box>();
        }
        idMap.put(id, box);
    }

    public Box getBoxById(final String id) {
        if (idMap == null) {
            idMap = new HashMap<String, Box>();
        }
        return idMap.get(id);
    }

    public void removeBoxId(final String id) {
        if (idMap != null) {
            idMap.remove(id);
        }
    }

    public Map<String, Box> getIdMap()
    {
        return idMap;
    }

    /**
     * Sets the textRenderer attribute of the RenderingContext object
     *
     * @param text_renderer The new textRenderer value
     */
    public void setTextRenderer(final TextRenderer text_renderer) {
        this.text_renderer = text_renderer;
    }// = "screen";

    /**

     * Set the current media type. This is usually something like <i>screen</i>
     * or <i>print</i> . See the <a href="http://www.w3.org/TR/CSS21/media.html">
     * media section</a> of the CSS 2.1 spec for more information on media
     * types.
     *
     * @param media The new media value
     */
    public void setMedia(final String media) {
        this.media = media;
    }

    /**
     * Gets the uac attribute of the RenderingContext object
     *
     * @return The uac value
     */
    public UserAgentCallback getUac() {
        return uac;
    }

    public UserAgentCallback getUserAgentCallback() {
        return uac;
    }

    public void setUserAgentCallback(final UserAgentCallback userAgentCallback) {
        final StyleReference styleReference = getCss();
        if (styleReference != null) {
            styleReference.setUserAgentCallback(userAgentCallback);
        }
        uac = userAgentCallback;
    }

    /**
     * Gets the dPI attribute of the RenderingContext object
     *
     * @return The dPI value
     */
    public float getDPI() {
        return this.dpi;
    }

    /**
     * Sets the effective DPI (Dots Per Inch) of the screen. You should normally
     * never need to override the dpi, as it is already set to the system
     * default by <code>Toolkit.getDefaultToolkit().getScreenResolution()</code>
     * . You can override the value if you want to scale the fonts for
     * accessibility or printing purposes. Currently the DPI setting only
     * affects font sizing.
     *
     * @param dpi The new dPI value
     */
    public void setDPI(final float dpi) {
        this.dpi = dpi;
        this.mm_per_dot = (CM__PER__IN * MM__PER__CM) / dpi;
    }

    /**
     * Gets the dPI attribute in a more useful form of the RenderingContext object
     *
     * @return The dPI value
     */
    public float getMmPerPx() {
        return this.mm_per_dot;
    }

    public FSFont getFont(final FontSpecificationI spec) {
        return getFontResolver().resolveFont(spec);
    }

    //strike-through offset should always be half of the height of lowercase x...
    //and it is defined even for fonts without 'x'!
    public float getXHeight(final FontContext fontContext, final FontSpecificationI fs) {
        final FSFont font = getFontResolver().resolveFont(fs);
        final FSFontMetrics fm = getTextRenderer().getFSFontMetrics(fontContext, font, " ");
        final float sto = fm.getStrikethroughOffset();
        return fm.getAscent() - 2 * Math.abs(sto) + fm.getStrikethroughThickness();
    }

    /**
     * Gets the baseURL attribute of the RenderingContext object
     *
     * @return The baseURL value
     */
    public String getBaseURL() {
        return _uri;
    }

    /**
     * Sets the baseURL attribute of the RenderingContext object
     *
     * @param url The new baseURL value
     */
    public void setBaseURL(final String url) {
        // TODO
    }

    /**
     * Returns true if the currently set media type is paged. Currently returns
     * true only for <i>print</i> , <i>projection</i> , and <i>embossed</i> ,
     * <i>handheld</i> , and <i>tv</i> . See the <a
     * href="http://www.w3.org/TR/CSS21/media.html">media section</a> of the CSS
     * 2.1 spec for more information on media types.
     *
     * @return The paged value
     */
    public boolean isPaged() {
        if (media.equals("print")) {
            return true;
        }
        if (media.equals("projection")) {
            return true;
        }
        if (media.equals("embossed")) {
            return true;
        }
        if (media.equals("handheld")) {
            return true;
        }
        if (media.equals("tv")) {
            return true;
        }
        return false;
    }

    public boolean isPrint() {
        return print;
    }

    public void setPrint(final boolean print) {
        this.print = print;
        setMedia(print ? "print" : "screen");
    }

    public void setFontResolver(final FontResolver resolver) {
        font_resolver = resolver;
    }

    public int getDotsPerPixel() {
        return dotsPerPixel;
    }

    public void setDotsPerPixel(final int pixelsPerDot) {
        this.dotsPerPixel = pixelsPerDot;
    }

    public CalculatedStyle getStyle(final Element e) {
        return getStyle(e, false);
    }

    public CalculatedStyle getStyle(final Element e, final boolean restyle) {
        if (styleMap == null) {
            styleMap = new HashMap<Element, CalculatedStyle>(1024, 0.75f);
        }

        CalculatedStyle result = null;
        if (! restyle) {
            result = styleMap.get(e);
        }
        if (result == null) {
            CalculatedStyle parentCalculatedStyle;
        	
        	if (e instanceof Document)
        	{
        		parentCalculatedStyle = new EmptyStyle();
        	}
        	else
        	{
        		final Node parent = e.getParentNode();

        		if (parent instanceof Document) {
        			parentCalculatedStyle = new EmptyStyle();
        		} else {
        			parentCalculatedStyle = getStyle((Element)parent, false);
        		}
        	}

            result = parentCalculatedStyle.deriveStyle(getCss().getCascadedStyle(getBaseURL(), e, restyle));

            styleMap.put(e, result);
        }

        return result;
    }

    public void reset() {
       styleMap = null;
       idMap = null;
       replacedElementFactory.reset();
    }

    @Deprecated
    public ReplacedElementFactory getReplacedElementFactory() {
        return replacedElementFactory;
    }

    @Deprecated
    public void setReplacedElementFactory(final ReplacedElementFactory ref) {
        if (this.replacedElementFactory != null) {
            this.replacedElementFactory.reset();
        }
        this.replacedElementFactory = ref;
    }
    
    public void setReplacedElementResolver(ReplacedElementResolver resolver)
    {
    	assert(resolver != null);
    	this.replacedElementResolver = resolver;
    }
    
    public ReplacedElementResolver getReplacedElementResolver()
    {
    	return this.replacedElementResolver;
    }

    public void removeElementReferences(final Element e) {
        final Optional<String> id = namespaceHandler.getID(e);
        if (id.isPresent() && !id.get().isEmpty()) {
            removeBoxId(id.get());
        }

        if (styleMap != null) {
            styleMap.remove(e);
        }

        getCss().removeStyle(e);

        if (e.hasChildNodes())
        {
        	NodeList nl = e.getChildNodes();
        	int length = nl.getLength();
        	
        	for (int i = 0; i < length; i++)
        	{
        		Node item = nl.item(i);
        		
        		if (item instanceof Element)
        			removeElementReferences((Element) item);
        	}
        }
    }

	public StylesheetInfo getDefaultStylesheet() 
	{
		return defaultStylesheet;
	}

	public void setDefaultStylesheet(final StylesheetInfo defaultStylesheet) 
	{
		this.defaultStylesheet = defaultStylesheet;
	}

	public boolean haveLookedUpDefaultStylesheet()
	{
		return lookedUpDefaultStylesheet;
	}

	public void setLookedUpDefaultStylesheet(final boolean lookedUpDefaultStylesheet)
	{
		this.lookedUpDefaultStylesheet = lookedUpDefaultStylesheet;
	}

	public Locale getLocale() {
		return localeTextBreaker;
	}

	public void setLocale(final Locale locale) {
		this.localeTextBreaker = locale;
	}

	public void setDocumentURI(String uri) 
	{
		_uri = uri;
	}
	
	public void setImageResolver(ImageResolver imgResolver)
	{
		this.imgResolver = imgResolver;
	}
		
	public FSImage resolveImage(ImageResourceI imgResource)
	{
		return imgResolver.resolveImage(imgResource.getImageUri(), imgResource.getImage());
	}

	public ImageResolver getImageResolver()
	{
		return imgResolver;
	}
}
