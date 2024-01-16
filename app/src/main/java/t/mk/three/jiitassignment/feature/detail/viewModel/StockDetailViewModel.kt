package t.mk.three.jiitassignment.feature.detail.viewModelimport androidx.lifecycle.ViewModelimport androidx.lifecycle.viewModelScopeimport kotlinx.coroutines.flow.MutableStateFlowimport kotlinx.coroutines.flow.SharingStartedimport kotlinx.coroutines.flow.StateFlowimport kotlinx.coroutines.flow.asStateFlowimport kotlinx.coroutines.flow.combineimport kotlinx.coroutines.flow.mapimport kotlinx.coroutines.flow.stateInimport kotlinx.coroutines.flow.updateimport kotlinx.coroutines.launchimport t.mk.three.jiitassignment.Rimport t.mk.three.jiitassignment.StockDetailQueryimport t.mk.three.jiitassignment.data.RankingApolloClientimport t.mk.three.jiitassignment.feature.detail.adapter.SignTableAdapterimport t.mk.three.jiitassignment.feature.detail.adapter.StockDetailAdapterimport t.mk.three.jiitassignment.feature.detail.repository.BaseStockDetailRepositoryimport t.mk.three.jiitassignment.feature.detail.view.StockDetailHeaderViewHolderimport t.mk.three.jiitassignment.type.FactorLevelimport kotlin.math.maximport kotlin.math.minprivate const val graphRangeInYear = 9class StockDetailViewModel(    private val repository: BaseStockDetailRepository) : ViewModel() {    private val _state = MutableStateFlow<State>(State.Init)    val canPullToRefresh = _state.asStateFlow().map {        when (it) {            is State.Error -> true            State.Init -> false            State.Loading -> false            is State.Success -> true        }    }    val tableData: StateFlow<List<Pair<String, List<SignTableAdapter.Item>>>> = _state.map {        when (it) {            is State.Success -> {                it.data.stock?.jitta?.sign?.last?.mapNotNull { data ->                    when (val display = data?.display) {                        is StockDetailQuery.DisplayIPODisplay -> null                        is StockDetailQuery.DisplayTableDisplay -> {                            val convertedList = buildList {                                add(                                    SignTableAdapter.Item.Header(                                        display.onDisplayTable.columnHead ?: emptyList()                                    )                                )                                display.onDisplayTable.columns?.forEach { column ->                                    add(                                        SignTableAdapter.Item.Detail(                                            header = column?.name ?: "",                                            values = column?.data ?: emptyList()                                        )                                    )                                }                            }                            Pair(data.title ?: "", convertedList)                        }                        is StockDetailQuery.OtherDisplay -> null                        null -> null                    }                } ?: emptyList()            }            is State.Error,            State.Init,            State.Loading -> {                emptyList()            }        }    }.stateIn(        scope = viewModelScope,        started = SharingStarted.WhileSubscribed(5000),        initialValue = emptyList()    )    private val expandIds = MutableStateFlow<List<String>>(emptyList())    val state = combine(        _state,        expandIds    ) { state, expandIds ->        when (state) {            is State.Error -> {                listOf(                    StockDetailAdapter.Item.Error(                        error = state.message                    )                )            }            State.Init,            State.Loading -> {                listOf(                    StockDetailAdapter.Item.Loading                )            }            is State.Success -> {                val items = mutableListOf<StockDetailAdapter.Item>()                val stock = state.data.stock                stock?.let {                    items.add(                        StockDetailAdapter.Item.Header(                            currency = it.currency ?: "",                            rank = it.comparison?.market?.rank ?: 0,                            member = it.comparison?.market?.member ?: 0,                            name = stock.title,                            symbol = stock.symbol,                            market = stock.market,                            score = stock.jitta?.score?.last?.value,                            jittaLine = it.jitta?.line?.values?.reversed() ?: emptyList(),                            priceLine = it.jitta?.monthlyPrice?.values?.reversed() ?: emptyList(),                            range = state.range,                            priceDiffPercent = stock.jitta?.priceDiff?.values ?: emptyList(),                            monthScore = stock.jitta?.score?.values ?: emptyList(),                        )                    )                }                stock?.jitta?.factor?.last?.value?.let { factor ->                    val factors = buildList {                        factor.growth?.let { data ->                            add(                                StockDetailAdapter.Item.Factor(                                    max = 100,                                    progress = (data.value ?: 0.0).toInt(),                                    title = data.name ?: "",                                    color = convertLevelToColorRes(data.level)                                )                            )                        }                        factor.recent?.let { data ->                            add(                                StockDetailAdapter.Item.Factor(                                    max = 100,                                    progress = (data.value ?: 0.0).toInt(),                                    title = data.name ?: "",                                    color = convertLevelToColorRes(data.level)                                )                            )                        }                        factor.financial?.let { data ->                            this.add(                                StockDetailAdapter.Item.Factor(                                    max = 100,                                    progress = (data.value ?: 0.0).toInt(),                                    title = data.name ?: "",                                    color = convertLevelToColorRes(data.level)                                )                            )                        }                        factor.`return`?.let { data ->                            add(                                StockDetailAdapter.Item.Factor(                                    max = 100,                                    progress = (data.value ?: 0.0).toInt(),                                    title = data.name ?: "",                                    color = convertLevelToColorRes(data.level)                                )                            )                        }                        factor.management?.let { data ->                            add(                                StockDetailAdapter.Item.Factor(                                    max = 100,                                    progress = (data.value ?: 0.0).toInt(),                                    title = data.name ?: "",                                    color = convertLevelToColorRes(data.level)                                )                            )                        }                    }                    if (factors.isNotEmpty()) {                        items.add(                            StockDetailAdapter.Item.Title(                                stringRes = R.string.jitta_factor_title                            )                        )                        items.addAll(factors)                    }                    stock.jitta.sign?.last?.let { sign ->                        items.add(                            StockDetailAdapter.Item.Title(                                stringRes = R.string.jitta_signs_title                            )                        )                        sign.forEach {                            when (val display = it?.display) {                                is StockDetailQuery.DisplayIPODisplay -> {                                    items.add(                                        StockDetailAdapter.Item.SignIPO(                                            title = display.onDisplayIPO.title,                                            value = display.onDisplayIPO.value,                                            currency = stock.currency                                        )                                    )                                }                                is StockDetailQuery.DisplayTableDisplay -> {                                    items.add(                                        StockDetailAdapter.Item.SignTable(                                            id = it.title ?: "",                                            title = it.title ?: "",                                            detail = it.value ?: "",                                            isExpand = expandIds.contains(it.title),                                            color = convertTypeToColorRes(it.type)                                        )                                    )                                }                                is StockDetailQuery.OtherDisplay,                                null -> {                                    //Do nothing                                }                            }                        }                    }                    stock.summary?.let {                        items.add(                            StockDetailAdapter.Item.Title(                                stringRes = R.string.description_title                            )                        )                        items.add(                            StockDetailAdapter.Item.Description(                                description = it                            )                        )                    }                    items.add(                        StockDetailAdapter.Item.Title(                            stringRes = R.string.company_info_title                        )                    )                    items.add(                        StockDetailAdapter.Item.CompanyInfo(                            sector = stock.sector?.name,                            industry = stock.industry,                            website = null                        )                    )                    stock.company?.link?.forEach {                        items.add(                            StockDetailAdapter.Item.CompanyInfo(                                sector = null,                                industry = null,                                website = it?.url.toString()                            )                        )                    }                }                items            }        }    }.stateIn(        scope = viewModelScope,        started = SharingStarted.WhileSubscribed(5000),        initialValue = listOf(            StockDetailAdapter.Item.Loading        )    )    private fun convertLevelToColorRes(level: FactorLevel?): Int {        return when (level) {            FactorLevel.HIGH -> R.color.green            FactorLevel.MEDIUM -> R.color.primaryColor            FactorLevel.LOW,            FactorLevel.UNKNOWN__ -> R.color.orange            null -> R.color.orange        }    }    private fun convertTypeToColorRes(type: String?): Int {        return when (type) {            "good" -> R.color.green            "bad" -> R.color.orange            else -> R.color.primaryColor        }    }    fun init(id: String) {        if (_state.value != State.Init) {            return        }        viewModelScope.launch {            _state.update {                State.Loading            }            when (val response = repository.getStockDetail(id)) {                is RankingApolloClient.Result.Error -> {                    _state.update {                        State.Error(                            response.error                        )                    }                }                is RankingApolloClient.Result.Success -> {                    val jittaLineFirstYear =                        response.response.stock?.jitta?.line?.values?.lastOrNull()?.year                    val priceFirstYear =                        response.response.stock?.jitta?.monthlyPrice?.values?.lastOrNull()?.year                    val lastYear = max(                        response.response.stock?.jitta?.line?.values?.firstOrNull()?.year ?: 0,                        response.response.stock?.jitta?.monthlyPrice?.values?.firstOrNull()?.year ?: 0                    )                    val firstYear = if (jittaLineFirstYear != null && priceFirstYear != null) {                        max(min(jittaLineFirstYear, priceFirstYear), lastYear - graphRangeInYear)                    } else {                        max(jittaLineFirstYear ?: priceFirstYear ?: 0, lastYear - graphRangeInYear)                    }                    val range = mutableListOf<StockDetailHeaderViewHolder.MonthYear>()                    if (lastYear > 0) {                        for (i in firstYear..lastYear) {                            for (j in 1..12) {                                range.add(                                    StockDetailHeaderViewHolder.MonthYear(                                        month = j,                                        year = i                                    )                                )                            }                        }                        range.add(                            StockDetailHeaderViewHolder.MonthYear(                                month = 1,                                year = lastYear + 1                            )                        )                    }                    _state.update {                        State.Success(                            data = response.response,                            range = range                        )                    }                }            }        }    }    fun updateExpandSign(id: String) {        expandIds.update { expandIds ->            if (expandIds.contains(id)) {                expandIds.filter { it != id }            } else {                expandIds.plus(id)            }        }    }    fun reload(id: String) {        _state.value = State.Init        init(id)    }    sealed class State {        data object Init : State()        data object Loading : State()        data class Error(            val message: String        ) : State()        data class Success(            val data: StockDetailQuery.Data,            val range: List<StockDetailHeaderViewHolder.MonthYear>        ) : State()    }}