package neelts.coubs.text

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.TextView

class TextExtraBold(context: Context?, attrs: AttributeSet?) : TextView(context, attrs) {

	init {
		this.typeface = Typeface.createFromAsset(context?.assets, "fonts/montserrat_extra_bold.ttf")
	}
}