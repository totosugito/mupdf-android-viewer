package com.artifex.mupdf.viewer;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.DisplayList;
import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Matrix;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.fitz.Rect;
import com.artifex.mupdf.fitz.RectI;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import java.util.ArrayList;

public class MuPDFCore
{
	private int resolution;
	private Document doc;
	private Outline[] outline;
	private int pageCount = -1;
	private int currentPage;
	private Page page;
	private float pageWidth;
	private float pageHeight;
	private DisplayList displayList;


	/* Default to "A Format" pocket book size. */
	private int layoutW = 312;
	private int layoutH = 504;
	private int layoutEM = 10;

	MuPDFCore(String filename) {
		doc = Document.openDocument(filename);
		doc.layout(layoutW, layoutH, layoutEM);
		pageCount = doc.countPages();
		resolution = 160;
		currentPage = -1;
	}

	MuPDFCore(byte[] buffer, String magic) {
		doc = Document.openDocument(buffer, magic);
		doc.layout(layoutW, layoutH, layoutEM);
		pageCount = doc.countPages();
		resolution = 160;
		currentPage = -1;
	}


	String getTitle() {
		return doc.getMetaData(Document.META_INFO_TITLE);
	}

	int countPages() {
		return pageCount;
	}

	synchronized boolean isReflowable() {
		return doc.isReflowable();
	}

	public synchronized int layout(int oldPage, int w, int h, int em) {
		if (w != layoutW || h != layoutH || em != layoutEM) {
			System.out.println("LAYOUT: " + w + "," + h);
			layoutW = w;
			layoutH = h;
			layoutEM = em;
			long mark = doc.makeBookmark(oldPage);
			doc.layout(layoutW, layoutH, layoutEM);
			currentPage = -1;
			pageCount = doc.countPages();
			outline = null;
			try {
				outline = doc.loadOutline();
			} catch (Exception ex) {
				/* ignore error */
			}
			return doc.findBookmark(mark);
		}
		return oldPage;
	}

	private synchronized void gotoPage(int pageNum) {
			/* TODO: page cache */
			if (pageNum > pageCount-1)
				pageNum = pageCount-1;
			else if (pageNum < 0)
				pageNum = 0;
			if (pageNum != currentPage) {
				currentPage = pageNum;
				if (page != null)
					page.destroy();
				page = null;
				if (displayList != null)
					displayList.destroy();
				displayList = null;
				page = doc.loadPage(pageNum);
				Rect b = page.getBounds();
				pageWidth = b.x1 - b.x0;
				pageHeight = b.y1 - b.y0;
			}
	}

	synchronized PointF getPageSize(int pageNum) {
		gotoPage(pageNum);
		return new PointF(pageWidth, pageHeight);
	}

	synchronized void onDestroy() {
		if (displayList != null)
			displayList.destroy();
		displayList = null;
		if (page != null)
			page.destroy();
		page = null;
		if (doc != null)
			doc.destroy();
		doc = null;
	}

	synchronized void drawPage(Bitmap bm, int pageNum,
							   int pageW, int pageH,
							   int patchX, int patchY,
							   Cookie cookie) {
			gotoPage(pageNum);

			if (displayList == null)
				displayList = page.toDisplayList(false);


				float zoom = resolution / 72;
				Matrix ctm = new Matrix(zoom, zoom);
				RectI bbox = new RectI(page.getBounds().transform(ctm));
				float xscale = (float)pageW / (float)(bbox.x1-bbox.x0);
				float yscale = (float)pageH / (float)(bbox.y1-bbox.y0);
				ctm.scale(xscale, yscale);

				AndroidDrawDevice dev = new AndroidDrawDevice(bm, patchX, patchY);
				displayList.run(dev, ctm, cookie);
				dev.close();
				dev.destroy();
			}





	synchronized void updatePage(Bitmap bm, int pageNum,
								 int pageW, int pageH,
								 int patchX, int patchY,
								 Cookie cookie) {
		drawPage(bm, pageNum, pageW, pageH, patchX, patchY, cookie);
	}

	synchronized Link[] getPageLinks(int pageNum) {
		gotoPage(pageNum);
		return page.getLinks();
	}

	synchronized Quad[] searchPage(int pageNum, String text) {
		gotoPage(pageNum);
		return page.search(text);
	}

	synchronized boolean hasOutline() {
		if (outline == null) {
			try {
				outline = doc.loadOutline();
			} catch (Exception ex) {
				/* ignore error */
			}
		}
		return outline != null;
	}

	private void flattenOutlineNodes(ArrayList<OutlineActivity.Item> result, Outline[] list, String indent) {
		for (Outline node : list) {
			if (node.title != null)
				result.add(new OutlineActivity.Item(indent + node.title, node.page));
			if (node.down != null)
				flattenOutlineNodes(result, node.down, indent + "    ");
		}
	}

	synchronized ArrayList<OutlineActivity.Item> getOutline() {
		ArrayList<OutlineActivity.Item> result = new ArrayList<OutlineActivity.Item>();
		flattenOutlineNodes(result, outline, "");
		return result;
	}

	synchronized boolean needsPassword() {
		return doc.needsPassword();
	}

	synchronized boolean authenticatePassword(String password) {
		return doc.authenticatePassword(password);
	}

	/*
	 * returns pdf page thumbnail bitmaps
	 */

	Bitmap[] getPDFThumbnails(int w, int h){

		int pageCount = countPages();
		Bitmap[] thumbnails = new Bitmap[pageCount];

		for (int i = 0; i < pageCount; i++) {
			PointF pageSize = getPageSize(i);
			float mSourceScale = Math.max(w/pageSize.x, h/pageSize.y);

			Point size = new Point((int)(pageSize.x*mSourceScale), (int)(pageSize.y*mSourceScale));
			final Bitmap bp = Bitmap.createBitmap(size.x,size.y, Bitmap.Config.ARGB_8888);

			drawPage(bp,i,size.x, size.y, 0, 0,new Cookie());
			thumbnails[i] = bp;
		}
		return thumbnails;
	}
}
