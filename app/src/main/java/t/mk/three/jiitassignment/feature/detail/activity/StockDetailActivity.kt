package t.mk.three.jiitassignment.feature.detail.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import t.mk.three.jiitassignment.R
import t.mk.three.jiitassignment.databinding.ActivityStockDetailBinding
import t.mk.three.jiitassignment.extension.thousands
import t.mk.three.jiitassignment.feature.detail.adapter.SignTableAdapter
import t.mk.three.jiitassignment.feature.detail.adapter.StockDetailAdapter
import t.mk.three.jiitassignment.feature.detail.viewModel.StockDetailViewModel

class StockDetailActivity : AppCompatActivity() {
    private val viewModel by viewModel<StockDetailViewModel>()

    private lateinit var binding: ActivityStockDetailBinding

    private val adapter by lazy {
        StockDetailAdapter(
            onSignClicked = { id ->
                viewModel.updateExpandSign(id)
            },
            getRecyclerView = {
                getRecyclerViewInternal(it)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.getStringExtra(STOCK_ID)?.let { id ->
            viewModel.init(id)
        } ?: finish()

        setupViewListener()
        observeData()
        setupActionBar()
    }

    private fun setupActionBar() {
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
            it.setBackgroundDrawable(ContextCompat.getDrawable(this, R.color.primaryColor))
            it.title = intent?.getStringExtra(STOCK_NAME)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupViewListener() {
        binding.rvContainer.adapter = adapter
        binding.rvContainer.layoutManager = LinearLayoutManager(this)
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.state.collect { items ->
                adapter.submitList(items)
            }
        }

        lifecycleScope.launch {
            viewModel.tableData.collect { items ->
                items.forEach {
                    (getRecyclerViewInternal(it.first).adapter as? SignTableAdapter)?.submitList(it.second)
                }
            }
        }
    }

    private val recyclerViewContainer: HashMap<String, RecyclerView> = hashMapOf()

    private fun getRecyclerViewInternal(id: String): RecyclerView {
        return recyclerViewContainer[id] ?: run {
            val newAdapter = SignTableAdapter()

            val newRecyclerView = RecyclerView(this).apply {
                val padding = resources.getDimensionPixelSize(R.dimen.spacing_12)

                setPadding(padding, 0, padding, 0)
                clipChildren = false
                clipToPadding = false

                this.layoutManager = LinearLayoutManager(binding.root.context, RecyclerView.HORIZONTAL, false)
                this.adapter = newAdapter
            }

            recyclerViewContainer[id] = newRecyclerView
            newRecyclerView
        }
    }

    companion object {
        private const val STOCK_ID = "STOCK_ID"
        private const val STOCK_NAME = "STOCK_NAME"

        fun getIntent(context: Context, id: String, name: String?) = Intent(context, StockDetailActivity::class.java).apply {
            putExtra(STOCK_ID, id)
            putExtra(STOCK_NAME, name)
        }
    }
}