package com.hecom.reporttable.table.format;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.LruCache;

import com.hecom.reporttable.R;
import com.hecom.reporttable.form.core.TableConfig;
import com.hecom.reporttable.form.data.CellInfo;
import com.hecom.reporttable.form.data.column.Column;
import com.hecom.reporttable.form.data.format.draw.ImageResDrawFormat;
import com.hecom.reporttable.form.utils.DensityUtils;
import com.hecom.reporttable.table.HecomTable;
import com.hecom.reporttable.table.bean.Cell;
import com.hecom.reporttable.table.lock.Locker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 绘制单元格
 */
public class CellDrawFormat extends ImageResDrawFormat<Cell> {

    private final HecomTextDrawFormat textDrawFormat;
    private final int drawPadding;
    private final Rect rect = new Rect();

    private final HecomTable table;

    private final Locker locker;

//    private final Rect imgRect;
//    private final Rect drawRect;

//    private final Paint imgPaint = new Paint();

    private final Cell.Icon lockIcon = new Cell.Icon();

    public LruCache<String,Bitmap> bitmapLruCache;
    public CellDrawFormat(final HecomTable table, Locker locker) {
        super(1, 1);
        this.table = table;
        textDrawFormat = new HecomTextDrawFormat(table, this);
        this.drawPadding = DensityUtils.dp2px(getContext(), 4);
        this.locker = locker;
//        imgRect = new Rect();
//        drawRect = new Rect();
//        imgPaint.setStyle(Paint.Style.FILL);
        lockIcon.setDirection(Cell.Icon.RIGHT);
        lockIcon.setWidth(DensityUtils.dp2px(getContext(), 15));
        lockIcon.setHeight(DensityUtils.dp2px(getContext(), 15));

        int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);// kB
        int cacheSize = maxMemory / 16;
        bitmapLruCache = new LruCache<String,Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key,Bitmap bitmap){
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;// KB
            }
        };
    }

    @Override
    protected Context getContext() {
        return table.getContext();
    }

    @Override
    protected Bitmap getBitmap(Cell cell, String value, int position) {
        String sUri = this.getResourceUri(cell,value,position);
        Bitmap bitmap = bitmapLruCache.get(sUri);
        if(bitmap == null) {
            if (String.valueOf(lockIcon.getResourceId()).equals(sUri)) {
                bitmap = BitmapFactory.decodeResource(getContext().getResources(),Integer.valueOf(sUri));
                if(bitmap !=null) {
                    bitmapLruCache.put(sUri, bitmap);
                }
            } else {
                int threadCount = 1;
                CountDownLatch latch = new CountDownLatch(threadCount);
                ExecutorService executor = Executors.newFixedThreadPool(threadCount);
                    executor.execute(new Runnable() {
                        public void run() {
                            InputStream in = null;
                            try {
                                in = new URL(sUri).openStream();
                                Bitmap innerBitmap = BitmapFactory.decodeStream(in);
                                innerBitmap = Bitmap.createScaledBitmap(innerBitmap, cell.getIcon().getWidth(), cell.getIcon().getHeight(), true);
                                if(innerBitmap !=null) {
                                    bitmapLruCache.put(sUri, innerBitmap);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            latch.countDown();
                        }
                    });

                // 在这里，主线程等待所有线程完成
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executor.shutdown();
                return bitmapLruCache.get(sUri);
            }
        }
        return bitmap;
    }

    private String getResourceUri(Cell cell, String value, int position) {
        Cell.Icon icon = cell.getIcon();
        // 调用到getResourceID时，一定是需要绘制icon的，如果icon为空，说明是锁定列
        if (position == 0 && icon == null) {
            return String.valueOf(lockIcon.getResourceId());
        }
        return icon.getPath().getUri();
    }

    @Override
    protected int getResourceID(Cell cell, String value, int position) {
        Cell.Icon icon = cell.getIcon();
        // 调用到getResourceID时，一定是需要绘制icon的，如果icon为空，说明是锁定列
        if (position == 0 && icon == null) {
            return lockIcon.getResourceId();
        }
        return icon.getResourceId();
    }


    @Override
    public int measureWidth(Column<Cell> column, int position, TableConfig config) {
        Cell.Icon icon = getIcon(column, position);
        int textWidth = textDrawFormat.measureWidth(column, position, config);
        int textPaddingLeft = column.getDatas().get(position).getTextPaddingLeft();
        if (icon == null) {
            return textWidth + Math.max(textPaddingLeft, 0);
        }
        if (icon.getDirection() == Cell.Icon.LEFT || icon.getDirection() == Cell.Icon.RIGHT) {
            return icon.getWidth() + textWidth + drawPadding + Math.max(textPaddingLeft, 0);
        } else {
            return Math.max(icon.getWidth(), textWidth) + Math.max(textPaddingLeft, 0);
        }
    }

    @Override
    public int measureHeight(Column<Cell> column, int position, TableConfig config) {
        Cell.Icon icon = getIcon(column, position);
        int textHeight = textDrawFormat.measureHeight(column, position, config);
        if (icon == null) {
            return textHeight;
        }
        if (icon.getDirection() == Cell.Icon.TOP || icon.getDirection() == Cell.Icon.BOTTOM) {
            return icon.getHeight() + textHeight + drawPadding;
        } else {
            return Math.max(icon.getHeight(), textHeight);
        }
    }

    @Override
    public void draw(Canvas c, Rect rect, CellInfo<Cell> cellInfo, TableConfig config) {

        Cell.Icon icon = getIcon(cellInfo.column, cellInfo.row);

        rect.left += (config.getLeftPadding(cellInfo)) * config.getZoom();
        rect.right -= config.getRightPadding() * config.getZoom();
        rect.top += config.getVerticalPadding() * config.getZoom();
        rect.bottom -= config.getVerticalPadding() * config.getZoom();

        if (icon == null) {
            textDrawFormat.draw(c, rect, cellInfo, config);
            return;
        }
        setImageWidth(icon.getWidth());
        setImageHeight(icon.getHeight());

        int imgWidth = (int) (getImageWidth() * config.getZoom());
        int imgHeight = (int) (getImageHeight() * config.getZoom());

        int textWidth, drawPadding = 0;
        if (!TextUtils.isEmpty(cellInfo.value.trim())) {
            drawPadding = (int) (this.drawPadding * config.getZoom());
        }
        Paint.Align textAlign;
        if (cellInfo.data.getTextAlignment() != null) {
            textAlign = cellInfo.data.getTextAlignment();
        } else {
            textAlign = table.getHecomStyle().getAlign();
        }
        int imgRight = 0, imgLeft = 0;
        switch (icon.getDirection()) {//单元格icon的相对位置
            case Cell.Icon.LEFT:
                this.rect.set(rect.left + (imgWidth + drawPadding), rect.top, rect.right,
                        rect.bottom);
                textDrawFormat.draw(c, this.rect, cellInfo, config);
                textWidth = getDrawWidth(cellInfo);
                switch (textAlign) { //单元格内容的对齐方式
                    case CENTER:
                        imgRight = Math.min(this.rect.right,
                                (this.rect.right + this.rect.left - textWidth) / 2) - drawPadding;
                        break;
                    case LEFT:
                        imgRight = this.rect.left - drawPadding;
                        break;
                    case RIGHT:
                        imgRight = this.rect.right - textWidth - drawPadding;
                        break;
                }
                this.rect.set(imgRight - imgWidth, rect.top, imgRight, rect.bottom);
                this.drawImg(c, this.rect, cellInfo, config);
                break;
            case Cell.Icon.RIGHT:
                this.rect.set(rect.left, rect.top, rect.right - (imgWidth + drawPadding),
                        rect.bottom);
                textDrawFormat.draw(c, this.rect, cellInfo, config);
                textWidth = getDrawWidth(cellInfo);
                switch (textAlign) { //单元格内容的对齐方式
                    case CENTER:
                        imgLeft = Math.min(this.rect.right,
                                (this.rect.right + this.rect.left + textWidth) / 2) + drawPadding;
                        break;
                    case LEFT:
                        imgLeft = this.rect.left + textWidth + drawPadding;
                        break;
                    case RIGHT:
                        imgLeft = this.rect.right + drawPadding;
                        break;
                }
                this.rect.set(imgLeft, rect.top, imgLeft + imgWidth, rect.bottom);
                this.drawImg(c, this.rect, cellInfo, config);
                break;
            case Cell.Icon.TOP:
                this.rect.set(rect.left, rect.top + (imgHeight + drawPadding) / 2, rect.right,
                        rect.bottom);
                textDrawFormat.draw(c, this.rect, cellInfo, config);
                int imgBottom = (rect.top + rect.bottom) / 2 - textDrawFormat.measureHeight
                        (cellInfo.column, cellInfo.row, config) / 2 + drawPadding;
                this.rect.set(rect.left, imgBottom - imgHeight, rect.right, imgBottom);
                this.drawImg(c, this.rect, cellInfo, config);
                break;
            case Cell.Icon.BOTTOM:
                this.rect.set(rect.left, rect.top, rect.right, rect.bottom - (imgHeight +
                        drawPadding) / 2);
                textDrawFormat.draw(c, this.rect, cellInfo, config);
                int imgTop = (rect.top + rect.bottom) / 2 + textDrawFormat.measureHeight
                        (cellInfo.column, cellInfo.row, config) / 2 - drawPadding;
                this.rect.set(rect.left, imgTop, rect.right, imgTop + imgHeight);
                this.drawImg(c, this.rect, cellInfo, config);
                break;

        }
    }

    private void drawImg(Canvas c, Rect rect, CellInfo<Cell> cellInfo, TableConfig config) {
        super.draw(c, rect, cellInfo, config);
    }

    private int getDrawWidth(CellInfo<Cell> cellInfo) {
        return (int) (cellInfo.data.getCache().getDrawWidth());
    }

    private Cell.Icon getIcon(Column<Cell> column, int position) {
        if (locker.needShowLock(position, column.getColumn())) {
            if (column.isFixed()) {
                lockIcon.setResourceId(R.mipmap.icon_lock);
            } else {
                lockIcon.setResourceId(R.mipmap.icon_unlock);
            }
            return lockIcon;
        } else {
            return column.getDatas().get(position).getIcon();
        }
    }
}
