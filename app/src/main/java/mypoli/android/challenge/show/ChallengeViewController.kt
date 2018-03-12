package mypoli.android.challenge.show

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import kotlinx.android.synthetic.main.controller_challenge.view.*
import kotlinx.android.synthetic.main.item_challenge_quest.view.*
import mypoli.android.MainActivity
import mypoli.android.R
import mypoli.android.challenge.QuestPickerViewController
import mypoli.android.challenge.edit.EditChallengeViewController
import mypoli.android.common.ViewUtils
import mypoli.android.common.redux.android.ReduxViewController
import mypoli.android.common.text.DateFormatter
import mypoli.android.common.view.*
import mypoli.android.quest.Quest
import mypoli.android.quest.RepeatingQuest
import mypoli.android.repeatingquest.show.RepeatingQuestViewController


/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 03/05/2018.
 */
class ChallengeViewController(args: Bundle? = null) :
    ReduxViewController<ChallengeAction, ChallengeViewState, ChallengeReducer>(args) {

    override val reducer = ChallengeReducer

    private lateinit var challengeId: String

    constructor(
        challengeId: String
    ) : this() {
        this.challengeId = challengeId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.controller_challenge, container, false)
        setToolbar(view.toolbar)
        view.collapsingToolbarContainer.isTitleEnabled = false

        setupAppBar(view)

        setupHistoryChart(view.progressChart)

        view.questList.layoutManager =
            LinearLayoutManager(container.context, LinearLayoutManager.VERTICAL, false)
        view.questList.adapter = QuestAdapter()

        view.addQuests.setOnClickListener {
            val changeHandler = FadeChangeHandler()
            rootRouter.pushController(
                RouterTransaction.with(QuestPickerViewController(challengeId))
                    .pushChangeHandler(changeHandler)
                    .popChangeHandler(changeHandler)
            )
        }

        return view
    }

    private fun setupAppBar(view: View) {
        view.appbar.addOnOffsetChangedListener(object :
            RepeatingQuestViewController.AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {

                appBarLayout.post {
                    if (state == State.EXPANDED) {
                        val supportActionBar = (activity as MainActivity).supportActionBar
                        supportActionBar?.setDisplayShowTitleEnabled(false)
                    } else if (state == State.COLLAPSED) {
                        val supportActionBar = (activity as MainActivity).supportActionBar
                        supportActionBar?.setDisplayShowTitleEnabled(true)
                    }
                }

            }
        })
    }

    private fun setupHistoryChart(chart: LineChart) {
        with(chart) {
            description = null
            setTouchEnabled(false)
            setPinchZoom(false)
            extraBottomOffset = 20f
            extraTopOffset = 20f

            setDrawGridBackground(false)
//            setDrawBarShadow(true)
//            setDrawValueAboveBar(false)

            axisRight.axisMinimum = 0f
            axisRight.axisMaximum = 100f
            axisRight.spaceTop = 0f
            axisRight.setValueFormatter { value, axis -> "${value.toInt()}%" }

            axisLeft.isEnabled = false


            xAxis.yOffset = ViewUtils.dpToPx(4f, activity!!)
            xAxis.isGranularityEnabled = true
            xAxis.granularity = 1f

            legend.isEnabled = false


        }

    }

    override fun onCreateLoadAction() = ChallengeAction.Load(challengeId)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.challenge_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {

            android.R.id.home ->
                router.handleBack()

            R.id.actionEdit -> {
                val changeHandler = FadeChangeHandler()
                rootRouter.pushController(
                    RouterTransaction.with(EditChallengeViewController(challengeId))
                        .pushChangeHandler(changeHandler)
                        .popChangeHandler(changeHandler)
                )
                true
            }
            R.id.actionDelete -> {
                dispatch(ChallengeAction.Remove(challengeId))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onAttach(view: View) {
        super.onAttach(view)
        showBackButton()
    }

    override fun onDetach(view: View) {
        (activity as MainActivity).supportActionBar?.setDisplayShowTitleEnabled(true)
        super.onDetach(view)
    }

    class XAxisValueFormatter(private val labels: List<String>) : IAxisValueFormatter {

        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            val idx = value.toInt()
            return if (idx < 0 || idx >= labels.size) {
                ""
            } else labels[idx]
        }
    }

    override fun render(state: ChallengeViewState, view: View) =
        when (state) {
            is ChallengeViewState.Changed -> {
                colorLayout(state, view)

                renderName(state.name, view)

                view.progress.progress = state.completedCount
                view.progress.secondaryProgress = state.completedCount
                view.progress.max = state.totalCount

                view.progressText.text = state.progressText

                view.difficulty.setCompoundDrawablesWithIntrinsicBounds(
                    IconicsDrawable(view.context)
                        .icon(GoogleMaterial.Icon.gmd_fitness_center)
                        .colorRes(R.color.md_white)
                        .sizeDp(24),
                    null, null, null
                )

                view.endDate.setCompoundDrawablesWithIntrinsicBounds(
                    IconicsDrawable(view.context)
                        .icon(MaterialDesignIconic.Icon.gmi_hourglass_outline)
                        .colorRes(R.color.md_white)
                        .sizeDp(24),
                    null, null, null
                )

                renderChart(state, view)
                renderQuests(state, view)
            }
            else -> {
            }
        }

    private fun renderQuests(state: ChallengeViewState.Changed, view: View) {
        (view.questList.adapter as QuestAdapter).updateAll(state.questViewModels)
    }

    private fun renderChart(state: ChallengeViewState.Changed, view: View) {
        view.progressChart.xAxis.setLabelCount(state.xAxisLabelCount, true)

        view.progressChart.xAxis.valueFormatter = XAxisValueFormatter(state.xAxisLabels)

        view.progressChart.data = createLineData(state.chartEntries)
        view.progressChart.invalidate()
        view.progressChart.animateX(1400, Easing.EasingOption.EaseInOutQuart)
    }

    private fun createLineData(entries: List<Entry>): LineData {

        val set = LineDataSet(entries, "")
        set.color = attrData(R.attr.colorAccent)
        set.lineWidth = ViewUtils.dpToPx(1f, activity!!)
        set.setDrawCircles(false)
        set.setDrawFilled(true)
        set.fillColor = attrData(R.attr.colorAccent)
        set.fillAlpha = 160
        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.setDrawValues(false)

        set.axisDependency = YAxis.AxisDependency.RIGHT

        val d = LineData()
        d.addDataSet(set)

        return d
    }

    private fun colorLayout(
        state: ChallengeViewState.Changed,
        view: View
    ) {
        view.appbar.setBackgroundColor(colorRes(state.color500))
        view.toolbar.setBackgroundColor(colorRes(state.color500))
        view.collapsingToolbarContainer.setContentScrimColor(colorRes(state.color500))
        activity?.window?.navigationBarColor = colorRes(state.color500)
        activity?.window?.statusBarColor = colorRes(state.color700)
    }

    private fun renderName(
        name: String,
        view: View
    ) {
        toolbarTitle = name
        view.name.text = name
    }

    data class QuestViewModel(
        val id: String,
        val name: String,
        @ColorRes val color: Int,
        @ColorRes val textColor: Int,
        val icon: IIcon,
        val isRepeating: Boolean,
        val isCompleted: Boolean
    )

    inner class QuestAdapter(private var viewModels: List<QuestViewModel> = listOf()) :
        RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount() = viewModels.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val vm = viewModels[position]
            val view = holder.itemView
            view.questName.text = vm.name
            view.questName.setTextColor(colorRes(vm.textColor))

            view.questIcon.backgroundTintList =
                ColorStateList.valueOf(colorRes(vm.color))
            view.questIcon.setImageDrawable(
                IconicsDrawable(view.context)
                    .icon(vm.icon)
                    .colorRes(R.color.md_white)
                    .sizeDp(22)
            )
            view.questRepeatIndicator.visible = vm.isRepeating
        }

        fun updateAll(viewModels: List<QuestViewModel>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_challenge_quest,
                    parent,
                    false
                )
            )

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private val ChallengeViewState.Changed.xAxisLabels
        get() = chartData.keys.map {
            DateFormatter.formatWithoutYear(activity!!, it)
        }

    private val ChallengeViewState.Changed.chartEntries
        get() = chartData.values.mapIndexed { i, value ->
            Entry(i.toFloat(), value.toFloat())
        }

    private val ChallengeViewState.Changed.color500
        get() = color.androidColor.color500

    private val ChallengeViewState.Changed.color700
        get() = color.androidColor.color700

    private val ChallengeViewState.Changed.progressText
        get() = "$completedCount of $totalCount ($progressPercent%) done"

    private val ChallengeViewState.Changed.questViewModels
        get() = quests.map {
            when (it) {
                is Quest -> QuestViewModel(
                    id = it.id,
                    name = it.name,
                    color = if (it.isCompleted) R.color.md_grey_300 else it.color.androidColor.color500,
                    textColor = if (it.isCompleted) R.color.md_dark_text_26 else R.color.md_dark_text_54,
                    icon = it.icon?.androidIcon?.icon ?: GoogleMaterial.Icon.gmd_local_florist,
                    isRepeating = false,
                    isCompleted = it.isCompleted
                )
                is RepeatingQuest -> QuestViewModel(
                    id = it.id,
                    name = it.name,
                    color = it.color.androidColor.color500,
                    textColor = if (it.isCompleted) R.color.md_dark_text_26 else R.color.md_dark_text_54,
                    icon = it.icon?.androidIcon?.icon ?: GoogleMaterial.Icon.gmd_local_florist,
                    isRepeating = true,
                    isCompleted = it.isCompleted
                )
            }

        }

}