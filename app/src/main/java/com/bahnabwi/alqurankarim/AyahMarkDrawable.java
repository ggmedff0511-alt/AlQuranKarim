package com.bahnabwi.alqurankarim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * AyahMarkDrawable - رسم علامة نهاية الآية المذهّبة.
 * تم تحديث التصميم جراحياً ليكون بشكل ثماني أضلاع ذهبي ليطابق هوية "المصحف الكوني" الفاخرة (التصميم الأصلي)،
 * مما يضمن اتساق التصميم بين الفهرس وشاشة القراءة.
 */
public class AyahMarkDrawable extends Drawable {
    // الألوان الذهبية المعتمدة في التصميم الأصلي
    private static final int GOLD_MAIN = Color.parseColor("#D9B24C"); 
    private static final int NUM_COLOR = Color.parseColor("#D9B24C"); 

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String label;
    private final int sizePx;

    public AyahMarkDrawable(Context c, int ayahNumber) {
        label = toArabicDigits(ayahNumber);
        // حجم العلامة 36dp لضمان الوضوح التام في كافة الهواتف
        sizePx = (int) (c.getResources().getDisplayMetrics().density * 36);
        
        fill.setColor(GOLD_MAIN);
        edge.setColor(GOLD_MAIN);
        edge.setStyle(Paint.Style.STROKE);
        ring.setColor(Color.WHITE);
        ring.setStyle(Paint.Style.STROKE);
        
        txt.setColor(NUM_COLOR);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTypeface(Typeface.DEFAULT_BOLD);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float r = Math.min(b.width(), b.height()) / 2.2f;

        // تعديل جراحي: رسم الزخرفة الذهبية الثمانية (مربعان متداخلان بزاوية 45 درجة) لتطابق التصميم الأصلي
        edge.setStrokeWidth(r * 0.15f);
        canvas.save();
        for (int i = 0; i < 2; i++) {
            canvas.drawRect(cx - r, cy - r, cx + r, cy + r, edge);
            canvas.rotate(45, cx, cy);
        }
        canvas.restore();
        
        // رسم حلقة داخلية رقيقة للجمالية والوضوح داخل النص
        ring.setStrokeWidth(r * 0.04f);
        canvas.drawCircle(cx, cy, r * 0.85f, ring);

        // الرقم في الوسط: يتم تعديل حجم النص تلقائياً ليتناسب مع المساحة المتاحة (خاصة للآيات مثل ٢٨٦)
        float size = r * 0.95f;
        txt.setTextSize(size);
        float maxW = r * 1.0f;
        while (txt.measureText(label) > maxW && size > 6f) {
            size -= 1f;
            txt.setTextSize(size);
        }
        
        Paint.FontMetrics fm = txt.getFontMetrics();
        canvas.drawText(label, cx, cy - (fm.ascent + fm.descent) / 2f, txt);
    }

    @Override public int getIntrinsicWidth()  { return sizePx; }
    @Override public int getIntrinsicHeight() { return sizePx; }
    @Override public void setAlpha(int a) { fill.setAlpha(a); edge.setAlpha(a); ring.setAlpha(a); txt.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { fill.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }

    /**
     * تحويل الأرقام الإنجليزية إلى أرقام عربية (المشرقية) للعرض في المصحف
     */
    public static String toArabicDigits(int n) {
        char[] d = {'٠','١','٢','٣','٤','٥','٦','٧','٨','٩'};
        StringBuilder sb = new StringBuilder();
        String s = String.valueOf(n);
        for (int i = 0; i < s.length(); i++) {
            int digit = s.charAt(i) - '0';
            if (digit >= 0 && digit <= 9) sb.append(d[digit]);
            else sb.append(s.charAt(i));
        }
        return sb.toString();
    }
}
