/**
 *  Page.java
 *
Copyright (c) 2014, Innovatics Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and / or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.github.pdfstream;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.*;
import java.util.*;

import javafx.geometry.Rectangle2D;


/**
 *  Used to create PDF page objects.
 *
 *  Please note:
 *  <pre>
 *  The coordinate (0f, 0f) is the top left corner of the page.
 *  The size of the pages are represented in points.
 *  1 point is 1/72 inches.
 *  </pre>
 *
 */
public class Page {

    protected PDF pdf;
    protected ByteArrayOutputStream buf;
    protected float[] tm = new float[] {1f, 0f, 0f, 1f};
    protected int renderingMode = 0;
    protected float width;
    protected float height;
    protected List<Integer> contents;
    protected List<Annotation> annots;
    protected List<Destination> destinations;
    protected float[] cropBox = null;
    protected float[] bleedBox = null;
    protected float[] trimBox = null;
    protected float[] artBox = null;

    private PdfColor pen = PdfGreyScaleColor.BLACK;
    private PdfColor brush = PdfGreyScaleColor.WHITE;
    private float pen_width = -1.0f;
    private int line_cap_style = 0;
    private int line_join_style = 0;
    private String linePattern = "[] 0";
    private Font font;
    private List<State> savedStates = new ArrayList<State>();
    private boolean _isPathOpen = false;


    public Page(float[] pageSize) throws Exception {
        this(null, pageSize);
    }


    /**
     *  Creates page object and add it to the PDF document.
     *
     *  Please note:
     *  <pre>
     *  The coordinate (0f, 0f) is the top left corner of the page.
     *  The size of the pages are represented in points.
     *  1 point is 1/72 inches.
     *  </pre>
     *
     *  @param pdf the pdf object.
     *  @param pageSize the page size of this page.
     */
    public Page(PDF pdf, float[] pageSize) throws Exception {
        this.pdf = pdf;
        contents = new ArrayList<Integer>();
        annots = new ArrayList<Annotation>();
        destinations = new ArrayList<Destination>();
        width = pageSize[0];
        height = pageSize[1];
        buf = new ByteArrayOutputStream(8192);

        if (pdf != null) {
            pdf.addPage(this);
        }
    }


    public byte[] getContent() {
        return buf.toByteArray();
    }

    public void pathOpen()
    {
    	_isPathOpen = true;
    }
    
    
    public void pathMoveTo(float x, float y)
    {
        append(x);
        append(' ');
        append(y);
        append(" m\n");
    }

    public void pathCloseSubpath()
    {
    	append('h');
    	append('\n');
    	_isPathOpen = false;
    }
    
    public void pathCurveTo(float x1, float y1, float x2, float y2, float x3, float y3)
    {
    	append(x1);
    	append(' ');
    	append(y1);
    	append(' ');
    	append(x2);
    	append(' ');
    	append(y2);
    	append(' ');
    	append(x3);
    	append(' ');
    	append(y3);
    	append(' ');
    	append('c');
    	append('\n');
    }
    
    public void pathCurveTo(float x1, float y1, float x3, float y3)
    {
    	append(x1);
    	append(' ');
    	append(y1);
    	append(' ');
    	append(x3);
    	append(' ');
    	append(y3);
    	append(' ');
    	append('y');
    	append('\n');
    }    

    public void pathLineTo(float x, float y)
    {
    	append(x);
    	append(' ');
    	append(y);
    	append(' ');
    	append('l');
    	append('\n');
    }
    
    /**
     *  Adds destination to this page.
     *
     *  @param name The destination name.
     *  @param yPosition The vertical position of the destination on this page.
     */
    public void addDestination(String name, float yPosition) {
        destinations.add(new Destination(name, height - yPosition));
    }


    protected void setDestinationsPageObjNumber(int pageObjNumber) {
        for (Destination destination : destinations) {
            destination.setPageObjNumber(pageObjNumber);
            this.pdf.destinations.put(destination.name, destination);
        }
    }

    
    /**
     *  Returns the width of this page.
     *
     *  @return the width of the page.
     */
    public float getWidth() {
        return width;
    }


    /**
     *  Returns the height of this page.
     *
     *  @return the height of the page.
     */
    public float getHeight() {
        return height;
    }


    /**
     *  Draws a line on the page, using the current color, between the points (x1, y1) and (x2, y2).
     *
     *  @param x1 the first point's x coordinate.
     *  @param y1 the first point's y coordinate.
     *  @param x2 the second point's x coordinate.
     *  @param y2 the second point's y coordinate.
     */
    public void drawLine(
            double x1,
            double y1,
            double x2,
            double y2) throws IOException {
        drawLine((float) x1, (float) y1, (float) x2, (float) y2);
    }


    /**
     *  Draws the text given by the specified string,
     *  using the specified main font and the current brush color.
     *  If the main font is missing some glyphs - the fallback font is used.
     *  The baseline of the leftmost character is at position (x, y) on the page.
     *
     *  @param font1 the main font.
     *  @param font2 the fallback font.
     *  @param str the string to be drawn.
     *  @param x the x coordinate.
     *  @param y the y coordinate.
     */
    public void drawString(
            Font font1,
            Font font2,
            String str,
            float x,
            float y) throws IOException {
        boolean usingFont1 = true;
        StringBuilder buf = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i++) {
            int ch = str.charAt(i);
            if ((font1.isCJK && ch >= 0x4E00 && ch <= 0x9FCC)
                    || (!font1.isCJK && font1.unicodeToGID[ch] != 0)) {
                if (!usingFont1) {
                    String s1 = buf.toString();
                    drawString(font2, s1, x, y);
                    x += font2.stringWidth(s1);
                    buf.setLength(0);
                    usingFont1 = true;
                }
            }
            else {
                if (usingFont1) {
                    String s1 = buf.toString();
                    drawString(font1, s1, x, y);
                    x += font1.stringWidth(s1);
                    buf.setLength(0);
                    usingFont1 = false;
                }
            }
            buf.append((char) ch);
        }

        if (usingFont1) {
            drawString(font1, buf.toString(), x, y);
        }
        else {
            drawString(font2, buf.toString(), x, y);
        }
    }


    /**
     *  Draws the text given by the specified string,
     *  using the specified font and the current brush color.
     *  The baseline of the leftmost character is at position (x, y) on the page.
     *
     *  @param font the font to use.
     *  @param str the string to be drawn.
     *  @param x the x coordinate.
     *  @param y the y coordinate.
     */
    public void drawString(
            Font font,
            String str,
            double x,
            double y) throws IOException {
        drawString(font, str, (float) x, (float) y);
    }


    /**
     *  Draws the text given by the specified string,
     *  using the specified font and the current brush color.
     *  The baseline of the leftmost character is at position (x, y) on the page.
     *
     *  @param font the font to use.
     *  @param str the string to be drawn.
     *  @param x the x coordinate.
     *  @param y the y coordinate.
     */
    public void drawString(
            Font font,
            String str,
            float x,
            float y) throws IOException {

        if (str == null || str.equals("")) {
            return;
        }

        append("q\n");  // Save the graphics state
        append("BT\n");

        if (font.fontID == null) {
            setTextFont(font);
        }
        else {
            append('/');
            append(font.fontID);
            append(' ');
            append(font.size);
            append(" Tf\n");
        }

        if (renderingMode != 0) {
            append(renderingMode);
            append(" Tr\n");
        }

        float skew = 0f;
        if (font.skew15 &&
                tm[0] == 1f &&
                tm[1] == 0f &&
                tm[2] == 0f &&
                tm[3] == 1f) {
            skew = 0.26f;
        }

        append(tm[0]);
        append(' ');
        append(tm[1]);
        append(' ');
        append(tm[2] + skew);
        append(' ');
        append(tm[3]);
        append(' ');
        append(x);
        append(' ');
        append(height - y);
        append(" cm\n");

        append("[ (");
        drawString(font, str);
        append(") ] TJ\n");
        append("ET\n");

        append("Q\n");  // Restore the graphics state
    }
    

    private void drawString(Font font, String str) throws IOException {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            int c1 = str.charAt(i);
            if (font.isComposite) {
                drawTwoByteChar(c1, font);
            }
            else {
                drawOneByteChar(c1, font, str, i);
            }
        }
    }


    private void drawTwoByteChar(int c1, Font font) throws IOException {
        if (c1 < font.firstChar || c1 > font.lastChar) {
            if (font.isCJK) {
                append((byte) 0x0000);
                append((byte) 0x0020);
            }
            else {
                append((byte) font.unicodeToGID[0x0000]);
                append((byte) font.unicodeToGID[0x0020]);
            }
        }
        else {
            byte hi;
            byte lo;
            if (font.isCJK) {
                hi = (byte) (c1 >> 8);
                lo = (byte) (c1);
            }
            else {
                int gid = font.unicodeToGID[c1];
                hi = (byte) (gid >> 8);
                lo = (byte) (gid);
            }
            if (hi == '(' || hi == ')' || hi == '\\') {
                append((byte) '\\');
            }
            append(hi);
    
            if (lo == '\r') {
                append("\\015");
            }
            else {
                if (lo == '(' || lo == ')' || lo == '\\') {
                    append((byte) '\\');
                }
                append(lo);
            }
        }
    }


    private void drawOneByteChar(int c1, Font font, String str, int i) throws IOException {
        if (c1 < font.firstChar || c1 > font.lastChar) {
            c1 = font.mapUnicodeChar(c1);
        }
        if (c1 == '(' || c1 == ')' || c1 == '\\') {
            append((byte) '\\');
        }
        append((byte) c1);

        if (font.isStandard && font.kernPairs && i < (str.length() - 1)) {
            c1 -= 32;
            int c2 = str.charAt(i + 1);
            if (c2 < font.firstChar || c2 > font.lastChar) {
                c2 = 32;
            }
            for (int j = 2; j < font.metrics[c1].length; j += 2) {
                if (font.metrics[c1][j] == c2) {
                    append(") ");
                    append(-font.metrics[c1][j + 1]);
                    append(" (");
                    break;
                }
            }
        }
    }

    /**
     * Sets the color for stroking operations.
     * The pen color is used when drawing lines and splines.
     */
    public void setPenColor(PdfColor color) 
    {
    	if (pen.equals(color))
    		return;
    	
    	color.setStrokeColorOnPage(this);
    	color.setAlphaColorOnPage(this);
    }


    /**
     * Sets the color for brush operations.
     * This is the color used when drawing regular text and filling shapes.
     */
    public void setBrushColor(PdfColor color) 
    {
    	if (brush.equals(color))
    		return;
    	
    	color.setNonStrokeColorOnPage(this);
    	color.setAlphaColorOnPage(this);
    }

    /**
     *  Sets the line width to the default.
     *  The default is the finest line width.
     */
    public void setDefaultLineWidth() throws IOException {
        if (pen_width != 0f) {
            pen_width = 0f;
            append(pen_width);
            append(" w\n");
        }
    }


    /**
     *  The line dash pattern controls the pattern of dashes and gaps used to stroke paths.
     *  It is specified by a dash array and a dash phase.
     *  The elements of the dash array are positive numbers that specify the lengths of
     *  alternating dashes and gaps.
     *  The dash phase specifies the distance into the dash pattern at which to start the dash.
     *  The elements of both the dash array and the dash phase are expressed in user space units.
     *  <pre>
     *  Examples of line dash patterns:
     *
     *      "[Array] Phase"     Appearance          Description
     *      _______________     _________________   ____________________________________
     *
     *      "[] 0"              -----------------   Solid line
     *      "[3] 0"             ---   ---   ---     3 units on, 3 units off, ...
     *      "[2] 1"             -  --  --  --  --   1 on, 2 off, 2 on, 2 off, ...
     *      "[2 1] 0"           -- -- -- -- -- --   2 on, 1 off, 2 on, 1 off, ...
     *      "[3 5] 6"             ---     ---       2 off, 3 on, 5 off, 3 on, 5 off, ...
     *      "[2 3] 11"          -   --   --   --    1 on, 3 off, 2 on, 3 off, 2 on, ...
     *  </pre>
     *
     *  @param pattern the line dash pattern.
     */
    public void setLinePattern(String pattern) {
        if (!pattern.equals(linePattern)) {
            linePattern = pattern;
            append(linePattern);
            append(" d\n");
        }
    }


    /**
     *  Sets the default line dash pattern - solid line.
     */
    public void setDefaultLinePattern() throws IOException {
        append("[] 0");
        append(" d\n");
    }


    /**
     *  Sets the pen width that will be used to draw lines and splines on this page.
     *
     *  @param width the pen width.
     */
    public void setPenWidth(double width) throws IOException {
        setPenWidth((float) width);
    }


    /**
     *  Sets the pen width that will be used to draw lines and splines on this page.
     *
     *  @param width the pen width.
     */
    public void setPenWidth(float width) {
        if (pen_width != width) {
            pen_width = width;
            append(pen_width);
            append(" w\n");
        }
    }


    /**
     *  Sets the current line cap style.
     *
     *  @param style the cap style of the current line. Supported values: Cap.BUTT, Cap.ROUND and Cap.PROJECTING_SQUARE
     */
    public void setLineCapStyle(int style) {
        if (line_cap_style != style) {
            line_cap_style = style;
            append(line_cap_style);
            append(" J\n");
        }
    }


    /**
     *  Sets the line join style.
     *
     *  @param style the line join style code. Supported values: Join.MITER, Join.ROUND and Join.BEVEL
     */
    public void setLineJoinStyle(int style) {
        if (line_join_style != style) {
            line_join_style = style;
            append(line_join_style);
            append(" j\n");
        }
    }

    /**
     *  Sets the text rendering mode.
     *
     *  @param mode the rendering mode.
     */
    public void setTextRenderingMode(int mode) throws Exception {
        if (mode >= 0 && mode <= 7) {
            this.renderingMode = mode;
        }
        else {
            throw new Exception("Invalid text rendering mode: " + mode);
        }
    }


    /**
     *  Sets the text direction.
     *
     *  @param degrees the angle.
     */
    public void setTextDirection(int degrees) throws Exception {
        if (degrees > 360) degrees %= 360;
        if (degrees == 0) {
            tm = new float[] { 1f,  0f,  0f,  1f};
        }
        else if (degrees == 90) {
            tm = new float[] { 0f,  1f, -1f,  0f};
        }
        else if (degrees == 180) {
            tm = new float[] {-1f,  0f,  0f, -1f};
        }
        else if (degrees == 270) {
            tm = new float[] { 0f, -1f,  1f,  0f};
        }
        else if (degrees == 360) {
            tm = new float[] { 1f,  0f,  0f,  1f};
        }
        else {
            float sinOfAngle = (float) Math.sin(degrees * (Math.PI / 180));
            float cosOfAngle = (float) Math.cos(degrees * (Math.PI / 180));
            tm = new float[] {cosOfAngle, sinOfAngle, -sinOfAngle, cosOfAngle};
        }
    }

    /**
     *  Sets the start of text block.
     *  Please see Example_32. This method must have matching call to setTextEnd().
     */
    public void setTextStart() throws IOException {
        append("BT\n");
    }


    /**
     *  Sets the text location.
     *  Please see Example_32.
     *
     *  @param x the x coordinate of new text location.
     *  @param y the y coordinate of new text location.
     */
    public void setTextLocation(float x, float y) throws IOException {
        append(x);
        append(' ');
        append(height - y);
        append(" Td\n");
    }


    public void setTextBegin(float x, float y) throws IOException {
        append("BT\n");
        append(x);
        append(' ');
        append(height - y);
        append(" Td\n");
    }


    /**
     *  Sets the text leading.
     *  Please see Example_32.
     *
     *  @param leading the leading.
     */
    public void setTextLeading(float leading) throws IOException {
        append(leading);
        append(" TL\n");
    }


    public void setCharSpacing(float spacing) throws IOException {
        append(spacing);
        append(" Tc\n");
    }


    public void setWordSpacing(float spacing) throws IOException {
        append(spacing);
        append(" Tw\n");
    }


    public void setTextScaling(float scaling) throws IOException {
        append(scaling);
        append(" Tz\n");
    }


    public void setTextRise(float rise) throws IOException {
        append(rise);
        append(" Ts\n");
    }


    public void setTextFont(Font font) throws IOException {
        this.font = font;
        append("/F");
        append(font.objNumber);
        append(' ');
        append(font.size);
        append(" Tf\n");
    }


    /**
     *  Prints a line of text and moves to the next line.
     *  Please see Example_32.
     */
    public void println(String str) throws IOException {
        print(str);
        println();
    }


    /**
     *  Prints a line of text.
     *  Please see Example_32.
     */
    public void print(String str) throws IOException {
        append('(');
        if (font != null && font.isComposite) {
            for (int i = 0; i < str.length(); i++) {
                int c1 = str.charAt(i);
                drawTwoByteChar(c1, font);
            }
        }
        else {
            for (int i = 0; i < str.length(); i++) {
                int ch = str.charAt(i);
                if (ch == '(' || ch == ')' || ch == '\\') {
                    append('\\');
                    append((byte) ch);
                }
                else if (ch == '\t') {
                    append(' ');
                    append(' ');
                    append(' ');
                    append(' ');
                }
                else {
                    append((byte) ch);
                }
            }
        }
        append(") Tj\n");
    }


    /**
     *  Move to the next line.
     *  Please see Example_32.
     */
    public void println() throws IOException {
        append("T*\n");
    }


    /**
     *  Sets the end of text block.
     *  Please see Example_32.
     */
    public void setTextEnd() throws IOException {
        append("ET\n");
    }


    public void save() {
        append("q\n");
        savedStates.add(new State(
                pen, brush, pen_width, line_cap_style, line_join_style, linePattern));
    }


    public void restore() {
        append("Q\n");
        if (savedStates.size() > 0) {
            State savedState = savedStates.remove(savedStates.size() - 1);
            pen = savedState.getPen();
            brush = savedState.getBrush();
            pen_width = savedState.getPenWidth();
            line_cap_style = savedState.getLineCapStyle();
            line_join_style = savedState.getLineJoinStyle();
            linePattern = savedState.getLinePattern();
        }
    }
    // <<


    /**
     * Sets the page CropBox.
     * See page 77 of the PDF32000_2008.pdf specification.
     *
     * @param upperLeftX the top left X coordinate of the CropBox.
     * @param upperLeftY the top left Y coordinate of the CropBox.
     * @param lowerRightX the bottom right X coordinate of the CropBox.
     * @param lowerRightY the bottom right Y coordinate of the CropBox.
     */
    public void setCropBox(
            float upperLeftX, float upperLeftY, float lowerRightX, float lowerRightY) {
        this.cropBox = new float[] {upperLeftX, upperLeftY, lowerRightX, lowerRightY};
    }


    /**
     * Sets the page BleedBox.
     * See page 77 of the PDF32000_2008.pdf specification.
     *
     * @param upperLeftX the top left X coordinate of the BleedBox.
     * @param upperLeftY the top left Y coordinate of the BleedBox.
     * @param lowerRightX the bottom right X coordinate of the BleedBox.
     * @param lowerRightY the bottom right Y coordinate of the BleedBox.
     */
    public void setBleedBox(
            float upperLeftX, float upperLeftY, float lowerRightX, float lowerRightY) {
        this.bleedBox = new float[] {upperLeftX, upperLeftY, lowerRightX, lowerRightY};
    }


    /**
     * Sets the page TrimBox.
     * See page 77 of the PDF32000_2008.pdf specification.
     *
     * @param upperLeftX the top left X coordinate of the TrimBox.
     * @param upperLeftY the top left Y coordinate of the TrimBox.
     * @param lowerRightX the bottom right X coordinate of the TrimBox.
     * @param lowerRightY the bottom right Y coordinate of the TrimBox.
     */
    public void setTrimBox(
            float upperLeftX, float upperLeftY, float lowerRightX, float lowerRightY) {
        this.trimBox = new float[] {upperLeftX, upperLeftY, lowerRightX, lowerRightY};
    }


    /**
     * Sets the page ArtBox.
     * See page 77 of the PDF32000_2008.pdf specification.
     *
     * @param upperLeftX the top left X coordinate of the ArtBox.
     * @param upperLeftY the top left Y coordinate of the ArtBox.
     * @param lowerRightX the bottom right X coordinate of the ArtBox.
     * @param lowerRightY the bottom right Y coordinate of the ArtBox.
     */
    public void setArtBox(
            float upperLeftX, float upperLeftY, float lowerRightX, float lowerRightY) {
        this.artBox = new float[] {upperLeftX, upperLeftY, lowerRightX, lowerRightY};
    }

    protected void append(String str) {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            buf.write((byte) str.charAt(i));
        }
    }


    protected void append(int num) {
        append(Integer.toString(num));
    }


    protected void append(float val) {
        append(PDF.df.format(val));
    }


    protected void append(char ch) {
        buf.write((byte) ch);
    }


    protected void append(byte b) {
        buf.write(b);
    }


    /**
     *  Appends the specified array of bytes to the page.
     */
    public void append(byte[] buffer) {
        try {
			buf.write(buffer);
		} catch (IOException e) {
			throw new PdfException(e);
		}
    }


    protected void drawString(
            Font font,
            String str,
            float x,
            float y,
            Map<String, PdfColor> colors) throws Exception {
        setTextBegin(x, y);
        setTextFont(font);

        StringBuilder buf1 = new StringBuilder();
        StringBuilder buf2 = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                printBuffer(buf2, colors);
                buf1.append(ch);
            }
            else {
                printBuffer(buf1, colors);
                buf2.append(ch);
            }
        }
        printBuffer(buf1, colors);
        printBuffer(buf2, colors);

        setTextEnd();
    }


    private void printBuffer(
            StringBuilder buf,
            Map<String, PdfColor> colors) throws Exception {
        String str = buf.toString();
        if (str.length() > 0) {
            if (colors.containsKey(str)) {
                setBrushColor(colors.get(str));
            }
            else {
                setBrushColor(PdfGreyScaleColor.BLACK);
            }
        }
        print(str);
        buf.setLength(0);
    }
    

    /**
     * Scale the coordinate space by sx and sy.
     */
    public void scale(float sx, float sy)
    {
    	// PDF Out: sx 0 0 sy 0 0 cm 
    	append(sx);
    	append(' ');
    	append(0);
    	append(' ');
    	append(0);
    	append(' ');
    	append(sy);
    	append(' ');
    	append(0);
    	append(' ');
    	append(0);
    	append(' ');
    	append("cm\n");
    }
    
    /**
     * Translate the coordinate space by tx and ty.
     */
    public void translate(float tx, float ty)
    {
    	// PDF Out: 1 0 0 1 tx ty cm
    	append(1);
    	append(' ');
    	append(0);
    	append(' ');
    	append(0);
    	append(' ');
    	append(1);
    	append(' ');
    	append(tx);
    	append(' ');
    	append(ty);
    	append(' ');
    	append("cm\n");
    }


	public void pathFillEvenOdd() 
	{
		if (_isPathOpen)
			pathCloseSubpath();
		
		append('f');
		append('*');
		append('\n');
	}


	public void pathFillNonZero() 
	{
		if (_isPathOpen)
			pathCloseSubpath();
		
		append('f');
		append('\n');
	}
	
	public void pathStroke()
	{
		if (_isPathOpen)
			pathCloseSubpath();

		append('S');
		append('\n');
	}


	public void pathClipEvenOdd() 
	{
		if (_isPathOpen)
			pathCloseSubpath();
		
		append('W');
		append('*');
		append('\n');
		append('n');
		append('\n');
	}


	public void pathClipNonZero()
	{
		if (_isPathOpen)
			pathCloseSubpath();
		
		append('W');
		append('\n');
		append('n');
		append('\n');
	}
}   // End of Page.java