package t.mk.three.jiitassignment.feature.main.activity

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import t.mk.three.jiitassignment.R
import t.mk.three.jiitassignment.databinding.ActivityMainBinding
import t.mk.three.jiitassignment.feature.detail.activity.StockDetailActivity
import t.mk.three.jiitassignment.feature.main.adapter.MainAdapter
import t.mk.three.jiitassignment.feature.main.viewModel.MainViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModel<MainViewModel>()

    private val mainAdapter by lazy {
        MainAdapter { id, name ->
            val intent = StockDetailActivity.getIntent(this, id, name)
            startActivity(intent)
        }
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewListener()
        observeData()

        if (savedInstanceState == null) {
            viewModel.init()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.items.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).collect {
                mainAdapter.submitList(it)
            }
        }

        lifecycleScope.launch {
            viewModel.market.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).collect {
                binding.sMarket.setSelection(it)
            }
        }

        lifecycleScope.launch {
            viewModel.selectedSector.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).collect {
                binding.sSector.setSelection(it)
            }
        }

        lifecycleScope.launch {
            viewModel.sectors.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .collect { sectors ->
                    binding.sSector.adapter = ArrayAdapter(
                        this@MainActivity,
                        R.layout.view_market_spinner_item,
                        sectors.map {
                            it.name
                        }
                    )
                }
        }

        lifecycleScope.launch {
            viewModel.canPullToRefresh.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .collect {
                    binding.srlContainer.isEnabled = it
                    binding.srlContainer.isRefreshing = it && binding.srlContainer.isRefreshing
                }
        }
    }

    private fun setupViewListener() {
        val layoutManager = LinearLayoutManager(this)
        binding.rvContainer.layoutManager = layoutManager
        binding.rvContainer.adapter = mainAdapter
        binding.rvContainer.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val loadMoreIndex = layoutManager.findLastVisibleItemPosition() + 2

                if (loadMoreIndex > (recyclerView.adapter?.itemCount ?: 0) && dy > 10) {
                    viewModel.loadMore()
                }
            }
        })

        binding.sMarket.adapter = ArrayAdapter(
            this,
            R.layout.view_market_spinner_item,
            MainViewModel.Filter.entries.map {
                it.getDropdownName()
            }
        )

        binding.sMarket.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                viewModel.setMarket(MainViewModel.Filter.entries[p2])
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //Do nothing
            }

        }

        binding.sSector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                viewModel.updateSector(viewModel.sectors.value[p2])
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //Do nothing
            }

        }

        binding.srlContainer.setOnRefreshListener {
            binding.srlContainer.isRefreshing = true
            viewModel.reload()
            binding.srlContainer.isRefreshing = false
        }
    }
}