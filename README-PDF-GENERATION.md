# How to Generate PDF from User Guide

This document explains how to convert the Employee Portal User Guide to PDF format.

## Option 1: Using Browser (Easiest)

1. Open `Employee_Portal_User_Guide.html` in your web browser (Chrome, Firefox, Edge, etc.)
2. Press `Ctrl+P` (Windows/Linux) or `Cmd+P` (Mac) to open the print dialog
3. Select **"Save as PDF"** as the destination printer
4. Click **"Save"** and choose a location
5. The PDF will be generated with proper page breaks and formatting

**Recommended Settings:**
- Paper size: A4
- Margins: Default or Custom (2cm recommended)
- Background graphics: Enabled (to show colors and styling)

## Option 2: Using Pandoc (Command Line)

If you have Pandoc installed:

```bash
# Convert Markdown to PDF (requires LaTeX)
pandoc Employee_Portal_User_Guide.md -o Employee_Portal_User_Guide.pdf --pdf-engine=xelatex

# Or convert HTML to PDF (requires wkhtmltopdf)
pandoc Employee_Portal_User_Guide.html -o Employee_Portal_User_Guide.pdf
```

## Option 3: Using Online Converters

1. Upload `Employee_Portal_User_Guide.html` to an online HTML to PDF converter
2. Popular options:
   - https://www.ilovepdf.com/html-to-pdf
   - https://www.freeconvert.com/html-to-pdf
   - https://www.adobe.com/acrobat/online/html-to-pdf.html

## Option 4: Using Microsoft Word

1. Open `Employee_Portal_User_Guide.html` in Microsoft Word
2. Word will convert the HTML to a Word document
3. Review and adjust formatting if needed
4. Go to **File** → **Save As** → Select **PDF** format
5. Save the PDF

## Recommended Approach

**For best results, use Option 1 (Browser Print to PDF)** as it preserves:
- Colors and styling
- Page breaks
- Table formatting
- All visual elements

The HTML file is already optimized for printing with:
- Page break controls
- Print-friendly styling
- Proper margins
- A4 page size settings
