package xyz.magicpro97.swipe

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val items = mutableListOf(
        Item("1"),
        Item("2"),
        Item("3"),
        Item("4"),
        Item("5")
    )
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var swipeController: SwipeHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        itemAdapter = ItemAdapter(items)
        rv.run {
            adapter = itemAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        swipeController = object : SwipeHelper(this@MainActivity, rv) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: ArrayList<UnderlayButton>
            ) {
                buffer.add(
                        UnderlayButton(
                            "Delete",
                            0,
                            Color.parseColor("#FF3C30"),
                            object : UnderlayButton.UnderlayButtonClickListener {
                                override fun onClick() {

                                }
                            }
                        ))
                buffer.add(
                    UnderlayButton(
                        "Delete",
                        0,
                        Color.parseColor("#FF3C30"),
                        object : UnderlayButton.UnderlayButtonClickListener {
                            override fun onClick() {

                            }
                        }
                    ))
            }
        }
    }
}
