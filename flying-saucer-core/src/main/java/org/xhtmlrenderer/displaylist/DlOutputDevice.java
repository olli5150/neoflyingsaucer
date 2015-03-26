package org.xhtmlrenderer.displaylist;

import java.awt.Rectangle;
import java.awt.RenderingHints.Key;
import java.awt.geom.Area;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSCMYKColor;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.derived.FSLinearGradient;
import org.xhtmlrenderer.css.style.derived.FSLinearGradient.StopValue;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.render.AbstractOutputDevice;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.BorderPainter;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.InlineLayoutBox;
import org.xhtmlrenderer.render.InlineText;
import org.xhtmlrenderer.render.PageBox;
import org.xhtmlrenderer.render.RenderingContext;

import com.github.neoflyingsaucer.displaylist.DlInstruction;
import com.github.neoflyingsaucer.displaylist.DlInstruction.DlRGBColor;
import com.github.neoflyingsaucer.displaylist.DlInstruction.Operation;
import com.github.neoflyingsaucer.extend.output.DisplayList;
import com.github.neoflyingsaucer.extend.output.FSFont;
import com.github.neoflyingsaucer.extend.output.FSGlyphVector;
import com.github.neoflyingsaucer.extend.output.FSImage;
import com.github.neoflyingsaucer.extend.output.JustificationInfo;
import com.github.neoflyingsaucer.extend.output.ReplacedElement;

public class DlOutputDevice extends AbstractOutputDevice implements OutputDevice 
{
	private final DisplayList dl;
    private Area clip;
    private Stroke stroke;
    private Object renderingHint = RenderingHints.VALUE_ANTIALIAS_DEFAULT;
    private Box _root;
    private final float dpi;
	
	public DlOutputDevice(DisplayList displayList, float dpi) 
	{
		this.dl = displayList;
		this.dpi = dpi;
	}

	public void drawString(String s, float x, float y)
	{
		dl.add(new DlInstruction.DlString(s, x, y));
	}
	
	public void drawString(String s, float x, float y, JustificationInfo info)
	{
		dl.add(new DlInstruction.DlStringEx(s, x, y, info));
	}
	
	public void drawGlyphVector(FSGlyphVector vec, float x, float y)
	{
		dl.add(new DlInstruction.DlGlyphVector(vec, x, y));
	}
	
	@Override
	public void setOpacity(float opacity) 
	{
		dl.add(new DlInstruction.DlOpacity(opacity));
	}

	@Override
    protected void drawLine(int x1, int y1, int x2, int y2) 
    {
    	dl.add(new DlInstruction.DlLine(x1, y1, x2, y2));
    }

	@Override
	public void translate(double tx, double ty) 
	{
		dl.add(new DlInstruction.DlTranslate(tx, ty));
	}

	@Override
	public void setStroke(Stroke s) 
	{
		if (!(s instanceof BasicStroke))
			return;
		
		BasicStroke basic = (BasicStroke) s;
		
		stroke = basic;
		
		dl.add(new DlInstruction.DlStroke(basic));
	}

    @Override
    public void setColor(final FSColor color) 
    {
        if (color instanceof FSRGBColor) 
        {
            FSRGBColor rgb = (FSRGBColor) color;
            dl.add(new DlInstruction.DlRGBColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), (int) (rgb.getAlpha() * 255)));
        }
        else if (color instanceof FSCMYKColor)
        {
        	FSCMYKColor cmyk = (FSCMYKColor) color;
        	dl.add(new DlInstruction.DlCMYKColor(cmyk.getCyan(), cmyk.getMagenta(), cmyk.getYellow(), cmyk.getBlack()));
        }
        else 
        {
        	assert(false);
        }
    }
	
	@Override
	public void fillRect(int x, int y, int width, int height) 
	{
		dl.add(new DlInstruction.DlRectangle(x, y, width, height, Operation.FILL));
	}
    
	@Override
	public void drawRect(int x, int y, int width, int height) 
	{
		dl.add(new DlInstruction.DlRectangle(x, y, width, height, Operation.STROKE));
	}
	
	@Override
	public void setClip(Shape s) 
	{
        if (s == null) 
            clip = null;
        else
            clip = new Area(s);
		
		dl.add(new DlInstruction.DlSetClip(s));
	}
	
	@Override
	public void clip(Shape s2) 
	{
        if (clip == null)
            clip = new Area(s2);
        else
            clip.intersect(new Area(s2));
		
		dl.add(new DlInstruction.DlClip(s2));
	}
	
	@Override
	public void drawOval(int x, int y, int width, int height) 
	{
		dl.add(new DlInstruction.DlOval(x, y, width, height, Operation.STROKE));
	}
	
	@Override
	public void fillOval(int x, int y, int width, int height) 
	{
		dl.add(new DlInstruction.DlOval(x, y, width, height, Operation.FILL));
	}
	
	@Override
	public void draw(Shape s) 
	{
		dl.add(new DlInstruction.DlDrawShape(s, Operation.STROKE));
	}
	
	@Override
	public void fill(Shape s) 
	{
		dl.add(new DlInstruction.DlDrawShape(s, Operation.FILL));
	}
	
	@Override
	public boolean isSupportsCMYKColors() 
	{
		return true;
	}
	
	@Override
	public Stroke getStroke()
	{
		return stroke;
	}

	@Override
	public Object getRenderingHint(Key key)
	{
		assert(key == RenderingHints.KEY_ANTIALIASING);
		return renderingHint;
	}

	@Override
	public void setRenderingHint(Key key, Object value) 
	{
		assert(key == RenderingHints.KEY_ANTIALIASING);
		assert(value == RenderingHints.VALUE_ANTIALIAS_DEFAULT || value == RenderingHints.VALUE_ANTIALIAS_OFF);
		
		if (value == RenderingHints.VALUE_ANTIALIAS_DEFAULT)
		{
			dl.add(new DlInstruction.DlAntiAliasDefault());
		}
		else
		{
			dl.add(new DlInstruction.DlAntiAliasOff());
		}
	}

	@Override
	public boolean isSupportsSelection()
	{
		return false;
	}

	@Override
	public void drawSelection(RenderingContext c, InlineText inlineText)
	{
		// NOT IMPLEMENTED: We no longer support a selection.
	}

	@Override
	public void paintReplacedElement(RenderingContext c, BlockBox box)
	{
		ReplacedElement replaced = box.getReplacedElement();
		dl.add(new DlInstruction.DlReplaced(replaced));
	}

	@Override
	public void setFont(FSFont font)
	{
		dl.add(new DlInstruction.DlFont(font));
	}

    @Override
    public void drawBorderLine(Rectangle bounds, int side, int lineWidth, boolean solid) 
    {
    	final int x = bounds.x;
        final int y = bounds.y;
        final int w = bounds.width;
        final int h = bounds.height;
        
        final int adj = solid ? 1 : 0;
        
        if (side == BorderPainter.TOP)
        {
            drawLine(x, y + (lineWidth / 2), x + w - adj, y + (lineWidth / 2));
        }
        else if (side == BorderPainter.LEFT) 
        {
            drawLine(x + (lineWidth / 2), y, x + (lineWidth / 2), y + h - adj);
        }
        else if (side == BorderPainter.RIGHT) 
        {
            int offset = (lineWidth / 2);

            if (lineWidth % 2 != 0)
            {
                offset += 1;
            }

            drawLine(x + w - offset, y, x + w - offset, y + h - adj);
        }
        else if (side == BorderPainter.BOTTOM)
        {
            int offset = (lineWidth / 2);

            if (lineWidth % 2 != 0)
            {
                offset += 1;
            }

            drawLine(x, y + h - offset, x + w - adj, y + h - offset);
        }
    }

	@Override
	public void drawBorderLine(Shape bounds, int side, int width, boolean solid)
	{
		draw(bounds);
	}

	@Override
	public void drawImage(FSImage image, int x, int y) 
	{
		dl.add(new DlInstruction.DlImage(image, x, y));
	}

	@Override
	public void drawLinearGradient(FSLinearGradient gradient, int x, int y, int width, int height)
	{
		DlInstruction.DlLinearGradient linear = new DlInstruction.DlLinearGradient(
				gradient.getStartX(), gradient.getStartY(), gradient.getEndX(), gradient.getEndY(), x, y, width, height);
		
		for (StopValue sv : gradient.getStopPoints())
		{
			if (sv.getColor() instanceof FSRGBColor)
			{
				FSRGBColor rgb = (FSRGBColor) sv.getColor();
				DlRGBColor rgba = new DlRGBColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), (int) (rgb.getAlpha() * 255));
				DlInstruction.DlStopPoint stopPoint = new DlInstruction.DlStopPoint(sv.getLength(), rgba);
				linear.stopPoints.add(stopPoint);
			}
			else
			{
				// TODO
			}
		}
		
		dl.add(linear);
	}

	@Override
	public Shape getClip() 
	{
		return clip;
	}
	
	@Override
    public void paintBackground(final RenderingContext c, final Box box) 
	{
        super.paintBackground(c, box);

        // TODO
        //processLink(c, box);
        
        if (!box.getStyle().isIdent(CSSName.FS_BOOKMARK_LEVEL, IdentValue.NONE))
        {
        	processBookmark(c, box);
        }
    }
	
	public void setRoot(Box box) 
	{
		_root = box;
	}
	
	private void processBookmark(RenderingContext c, Box box)
	{
		// Continuous renderer does not support bookmarks currently.
		if (_root.getLayer().getPages().isEmpty())
    		return;
		
		int bookmarkLevel = (int) box.getStyle().asFloat(CSSName.FS_BOOKMARK_LEVEL);

    	String bookmarkContent = box.getElement().getTextContent();
 
    	PageBox page = _root.getLayer().getPage(c, getPageRefY(box));
    	
        if (page != null)
        {
            int distanceFromTop = page.getMarginBorderPadding(c, CalculatedStyle.TOP);
            distanceFromTop += box.getAbsY() + box.getMargin(c).top() - page.getTop();
            
            DlInstruction.DlBookmark dlBookmark = new DlInstruction.DlBookmark(bookmarkLevel, page.getHeight(c) / dpi - distanceFromTop, 
            		bookmarkContent, page.getPageNo());
            
            dl.add(dlBookmark);
        }
	}
	
    private int getPageRefY(Box box) 
    {
        if (box instanceof InlineLayoutBox) 
        {
            InlineLayoutBox iB = (InlineLayoutBox) box;
            return iB.getAbsY() + iB.getBaseline();
        }
        else 
        {
            return box.getAbsY();
        }
    }
}