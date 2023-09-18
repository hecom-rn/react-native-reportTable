package com.hecom.reporttable.form.data.format.draw;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;

import com.hecom.reporttable.TableUtil;
import com.hecom.reporttable.form.core.TableConfig;
import com.hecom.reporttable.form.data.CellInfo;
import com.hecom.reporttable.form.data.column.Column;
import com.hecom.reporttable.form.data.format.bg.ICellBackgroundFormat;
import com.hecom.reporttable.form.utils.DrawUtils;
import com.hecom.reporttable.table.bean.JsonTableBean;
import com.hecom.reporttable.table.bean.TypicalCell;

import java.lang.ref.SoftReference;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huang on 2017/10/30.
 */

public class TextDrawFormat<T> implements IDrawFormat<T> {


    private Map<String, SoftReference<String[]>> valueMap; //避免产生大量对象

    public TextDrawFormat() {
        valueMap = new HashMap<>();
    }

    private static final String TAG = "TextDrawFormat";
    int asteriskRight, asteriskLeft;
    float textWidth;

    private Rect contentReact = new Rect();
    private Rect asteriskReact = new Rect();

    @Override
    public int measureWidth(Column<T> column, TypicalCell cell, TableConfig config) {
        Paint paint = config.getPaint();
        config.getContentStyle().fillPaint(paint);
        int iconSpace = TableUtil.calculateIconWidth(config, cell.columnIndex, cell.rowIndex);
        float asteriskWidth = TableUtil.calculateAsteriskWidth(config, cell.columnIndex, cell.rowIndex);
        String text = getWrapText(column, cell.jsonTableBean.title, paint, config, (int) (iconSpace + asteriskWidth), -1);
//        column.setFormatData(position,value);
        return DrawUtils.getMultiTextWidth(paint, getSplitString(text));
    }

    @Override
    public int measureHeight(Column<T> column, TypicalCell cell, TableConfig config, int sepcWidth) {
        Paint paint = config.getPaint();
        config.getContentStyle().fillPaint(paint);
        int iconSpace = TableUtil.calculateIconWidth(config, cell.columnIndex, cell.rowIndex);
        float asteriskWidth = TableUtil.calculateAsteriskWidth(config, cell.columnIndex, cell.rowIndex);
        String text = getWrapText(column, cell.jsonTableBean.title, paint, config, (int) (iconSpace + asteriskWidth), sepcWidth);
        return DrawUtils.getMultiTextHeight(paint, getSplitString(text)) + 40;
    }

    @Override
    public int measureWidth(Column<T> column, int position, TableConfig config, boolean onlyCalculate, int specWidth) {
        Paint paint = config.getPaint();
        config.getContentStyle().fillPaint(paint);
        String value = column.getCacheWrapText(position);
        if (null == value) {
            int iconSpace = TableUtil.calculateIconWidth(config, column.getColumn(), position);
            float asteriskWidth = TableUtil.calculateAsteriskWidth(config, column.getColumn(), position);
            value = getWrapText(column, column.format(position), paint, config, (int) (iconSpace + asteriskWidth), specWidth);
        }
        if (!onlyCalculate) {
            column.setFormatData(position, value);
        }
        return DrawUtils.getMultiTextWidth(paint, getSplitString(value));
    }


    @Override
    public int measureHeight(Column<T> column, int position, TableConfig config) {
        Paint paint = config.getPaint();
        config.getContentStyle().fillPaint(paint);
        String value = column.getCacheWrapText(position);
        if (null == value) {
            int iconSpace = TableUtil.calculateIconWidth(config, column.getColumn(), position);
            float asteriskWidth = TableUtil.calculateAsteriskWidth(config, column.getColumn(), position);
            value = getWrapText(column, column.format(position), paint, config, (int) (iconSpace + asteriskWidth), -1);
        }
        return DrawUtils.getMultiTextHeight(paint, getSplitString(value)) + config.dp8 * 2;
    }

    @Override
    public float draw(Canvas c, Rect rect, CellInfo<T> cellInfo, TableConfig config) {
        int asteriskWidth = (int) (TableUtil.calculateAsteriskWidth(config, cellInfo.col, cellInfo.row) * config.getZoom());
        //  表头必填项增加必填符号*;左对齐字段*号放右边，右对产/居中对产字段*方左边
        Paint paint = config.getPaint();
        setTextPaint(config, cellInfo, paint);
        if (asteriskWidth > 0) {
            Paint.Align textAlign = TableUtil.getAlignConfig(config, cellInfo.row, cellInfo.col);
            if (textAlign == null) textAlign = Paint.Align.CENTER;
            switch (textAlign) { //单元格内容的对齐方式
                case CENTER:
                    this.contentReact.set(rect.left + asteriskWidth, rect.top, rect.right, rect.bottom);
                    textWidth = drawText(c, cellInfo, this.contentReact, paint, config, 0);
                    asteriskRight = (int) ((this.contentReact.right + this.contentReact.left - textWidth) / 2);
                    this.asteriskReact.set(asteriskRight - asteriskWidth, rect.top, asteriskRight, rect.bottom);
                    this.drawAsterisk(c, this.asteriskReact, cellInfo, config);
                    break;
                case LEFT:
                    this.contentReact.set(rect.left, rect.top, rect.right - asteriskWidth, rect.bottom);
                    textWidth = drawText(c, cellInfo, this.contentReact, paint, config, 0);
                    asteriskLeft = (int) (this.contentReact.left + textWidth);
                    this.asteriskReact.set(asteriskLeft, rect.top, asteriskLeft + asteriskWidth, rect.bottom);
                    this.drawAsterisk(c, this.asteriskReact, cellInfo, config);

                    break;
                case RIGHT:
                    this.contentReact.set(rect.left + asteriskWidth, rect.top, rect.right, rect.bottom);
                    textWidth = drawText(c, cellInfo, this.contentReact, paint, config, 0);
                    asteriskRight = (int) (this.contentReact.right - textWidth);
                    this.asteriskReact.set(asteriskRight - asteriskWidth, rect.top, asteriskRight, rect.bottom);
                    this.drawAsterisk(c, this.asteriskReact, cellInfo, config);
                    break;
            }
            return textWidth + asteriskWidth;
        } else {
            return drawText(c, cellInfo, rect, paint, config, 0);
        }
    }

    private float drawAsterisk(Canvas c, Rect rect, CellInfo<T> cellInfo, TableConfig config) {
        Paint asteriskPaint = config.getAsteriskPaint();
        JsonTableBean jsonTableBean = config.getTabArr()[cellInfo.row][cellInfo.col];
        String asteriskColor = jsonTableBean.getAsteriskColor();
        int textSize = (jsonTableBean.getFontSize() != null && jsonTableBean.getFontSize().compareTo(0) > 0) ? jsonTableBean.getFontSize() : config.getContentStyle().getTextSize();
        asteriskPaint.setTextSize(textSize * config.getZoom());
        asteriskPaint.setColor(Color.parseColor(asteriskColor));
        return DrawUtils.getMultiTextWidth(asteriskPaint, config.ASTERISK_ARRAY);
    }

    protected float drawText(Canvas c, CellInfo<T> cellInfo, Rect rect, Paint paint, TableConfig config, int marginRight) {
        String value = cellInfo.wrapFlag ? cellInfo.value : getWrapText(cellInfo.value, paint, config, marginRight, rect);
        String[] values = getSplitString(value);
        DrawUtils.drawMultiText(c, paint, rect, values);
        return DrawUtils.getMultiTextWidth(paint, values);
    }


    public void setTextPaint(TableConfig config, CellInfo<T> cellInfo, Paint paint) {
        JsonTableBean jsonTableBean = config.getTabArr()[cellInfo.row][cellInfo.col];
        config.getContentStyle().fillPaint(paint);
        ICellBackgroundFormat<CellInfo> backgroundFormat = config.getContentCellBackgroundFormat();
        if (backgroundFormat != null && backgroundFormat.getTextColor(cellInfo) != TableConfig.INVALID_COLOR) {
            paint.setColor(backgroundFormat.getTextColor(cellInfo));
        }
        paint.setTextSize(paint.getTextSize() * config.getZoom() * config.getPartlyCellZoom());
        paint.setFakeBoldText(config.getTabArr()[cellInfo.row][cellInfo.col].isOverstriking);
        paint.setTextAlign(TableUtil.getAlignConfig(config, cellInfo.row, cellInfo.col));
        paint.setStrikeThruText(null==jsonTableBean.strikethrough?false:jsonTableBean.strikethrough);
    }

    protected String[] getSplitString(String val) {
        String[] values = null;
        if (valueMap.get(val) != null) {
            values = valueMap.get(val).get();
        }
        if (values == null) {
            values = val.split("\n");

            valueMap.put(val, new SoftReference<>(values));
        }
        return values;
    }


    public String getWrapText(Column column, String value, Paint paint, TableConfig config, int marginRight, int specWidth) {
        int paddingLeftSize = config.getTextLeftOffset();
        int paddingRightSize = config.getTextRightOffset();
        int maxWidth = specWidth < 0 ? column.getMaxWidth() : specWidth;
        return getWrapText(value, paint, marginRight, paddingLeftSize, paddingRightSize, maxWidth);
    }

    public String getWrapText(String value, Paint paint, TableConfig config, int marginRight, Rect rect) {
        int maxWidth = rect.right - rect.left;
        return getWrapText(value, paint, 0, 0, 0, maxWidth);
    }

    private String getWrapText(String value, Paint paint, int marginRight, int paddingLeftSize, int paddingRightSize, int maxWidth) {
        if (TextUtils.isEmpty(value)) return value;
        if (maxWidth <= 0) {
            return value;
        } else {
            float strLen = paint.measureText(value);
            int leeway = paddingLeftSize + paddingRightSize + (marginRight > 0 ? marginRight : 0);
            float expect = strLen + leeway;
            float realWidth = expect > maxWidth
                    ? maxWidth - leeway
                    : expect - leeway;
            StringBuilder stringBuilder = new StringBuilder();

            BreakIterator breakIterator = BreakIterator.getCharacterInstance();
            breakIterator.setText(value);

            String temp = "";
            String curLineStr = "";
            int start = breakIterator.first();
            int end = breakIterator.next();
            while (end != BreakIterator.DONE) {
                temp = value.substring(start, end);
                float tempStrLen = paint.measureText(temp);
                if (tempStrLen <= realWidth) {
                    curLineStr = temp;
                    if (curLineStr.endsWith("\n")) {
                        stringBuilder.append(curLineStr);
                        curLineStr = "";
                        start = end;
                    }
                } else {
                    stringBuilder.append(curLineStr);
                    stringBuilder.append("\n");
                    start = end - 1;
                    continue;
                }
                end = breakIterator.next();
            }
            stringBuilder.append(curLineStr);
            return stringBuilder.toString();
        }
    }
}
