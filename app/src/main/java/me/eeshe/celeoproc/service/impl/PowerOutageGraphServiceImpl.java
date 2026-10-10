package me.eeshe.celeoproc.service.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.eeshe.celeoproc.config.AppSettings;
import me.eeshe.celeoproc.config.Message;
import me.eeshe.celeoproc.model.PowerOutageLog;
import me.eeshe.celeoproc.service.MessageService;
import me.eeshe.celeoproc.service.NicknameResolver;
import me.eeshe.celeoproc.service.PowerOutageGraphService;

/**
 * Renders the power outage charts with JFreeChart.
 *
 * <p>
 * Layout shared by both chart kinds: days of the month on the X axis, hours of
 * the day (0..24) on the Y axis, and each outage segment drawn as a vertical
 * floating bar spanning its start and end hour. Outages crossing midnight are
 * split so every segment belongs to a single day.
 */
public final class PowerOutageGraphServiceImpl implements PowerOutageGraphService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowerOutageGraphServiceImpl.class);

    private static final Path DEFAULT_BASE_DIR = Path.of("data", "graphs");
    private static final double SINGLE_BAR_HALF_WIDTH = 0.35;
    /**
     * Fraction of a 1.0-wide day slot used by the user columns in the combined
     * chart.
     */
    private static final double DAY_GROUP_FILL = 0.8;
    private static final int IMAGE_WIDTH = 1200;
    private static final int IMAGE_HEIGHT = 800;
    private static final int COMBINED_IMAGE_WIDTH = 4000;
    private static final int COMBINED_IMAGE_HEIGHT = 900;

    private static final Color CHART_BACKGROUND = new Color(0x1E1F22);
    private static final Color PLOT_BACKGROUND = new Color(0x2B2D31);
    private static final Color GRIDLINE_COLOR = new Color(0x3F4147);
    private static final Color DAY_SEPARATOR_COLOR = new Color(0x4E5058);
    private static final Color AXIS_TEXT_COLOR = new Color(0xDBDEE1);
    private static final Color TITLE_TEXT_COLOR = new Color(0xF2F3F5);
    private static final Color BAR_OUTLINE_COLOR = new Color(0x111214);

    private final AppSettings appSettings;
    private final NicknameResolver nicknameResolver;
    private final MessageService messageService;
    private final Path baseDir;

    /**
     * Creates the service, writing its graphs under the default
     * {@code data/graphs} directory.
     *
     * @param appSettings      settings providing the timezone used to place the
     *                         outages on the charts
     * @param nicknameResolver resolver used for the per-user chart labels
     * @param messageService   source of the configurable chart titles and axis
     *                         labels
     */
    public PowerOutageGraphServiceImpl(
            final AppSettings appSettings,
            final NicknameResolver nicknameResolver,
            final MessageService messageService) {
        this(appSettings, nicknameResolver, messageService, DEFAULT_BASE_DIR);
    }

    /**
     * Creates the service with an explicit output directory.
     *
     * @param appSettings      settings providing the timezone used to place the
     *                         outages on the charts
     * @param nicknameResolver resolver used for the per-user chart labels
     * @param messageService   source of the configurable chart titles and axis
     *                         labels
     * @param baseDir          directory the per-invocation graph folders are
     *                         created in
     */
    PowerOutageGraphServiceImpl(
            final AppSettings appSettings,
            final NicknameResolver nicknameResolver,
            final MessageService messageService,
            final Path baseDir) {
        this.appSettings = Objects.requireNonNull(appSettings, "AppSettings must not be null");
        this.nicknameResolver = Objects.requireNonNull(nicknameResolver, "NicknameResolver must not be null");
        this.messageService = Objects.requireNonNull(messageService, "MessageService must not be null");
        this.baseDir = Objects.requireNonNull(baseDir, "Base directory must not be null");
    }

    @Override
    public List<Path> generateGraphs(final long guildId, final List<PowerOutageLog> logs) {
        Objects.requireNonNull(logs, "Logs must not be null");
        if (logs.isEmpty()) {
            return List.of();
        }
        final Path outputDir = baseDir.resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(outputDir);
        } catch (final IOException exception) {
            LOGGER.error("Failed to create graph output directory '{}'", outputDir, exception);
            return List.of();
        }
        final Map<Long, List<PowerOutageLog>> logsByUser = groupLogsByUser(logs);
        final List<Long> userIds = new ArrayList<>(logsByUser.keySet());
        final Map<Long, String> nicknames = nicknameResolver.resolveNicknames(guildId, userIds);
        final Map<Long, Color> userColors = buildUserColors(userIds);

        final Map<Long, Map<YearMonth, List<Segment>>> segmentsByUserMonth = new LinkedHashMap<>();
        for (final Map.Entry<Long, List<PowerOutageLog>> entry : logsByUser.entrySet()) {
            segmentsByUserMonth.put(entry.getKey(), splitByMonth(entry.getValue()));
        }
        final List<Path> generated = new ArrayList<>();
        generated.addAll(writeIndividualCharts(outputDir, segmentsByUserMonth, nicknames, userColors));
        generated.addAll(writeCombinedCharts(outputDir, segmentsByUserMonth, nicknames, userColors));
        return generated;
    }

    @Override
    public void deleteGraphs(final List<Path> graphs) {
        if (graphs == null || graphs.isEmpty()) {
            return;
        }

        final Set<Path> directories = new LinkedHashSet<>();
        for (final Path graph : graphs) {
            try {
                Files.deleteIfExists(graph);
            } catch (final IOException exception) {
                LOGGER.warn("Failed to delete graph '{}'", graph, exception);
            }
            if (graph.getParent() != null) {
                directories.add(graph.getParent());
            }
        }

        for (final Path directory : directories) {
            try {
                Files.deleteIfExists(directory);
            } catch (final DirectoryNotEmptyException exception) {
                // Other graph files still live in it; a later cleanup deletes it.
            } catch (final IOException exception) {
                LOGGER.warn("Failed to delete graph directory '{}'", directory, exception);
            }
        }
    }

    /** Groups the logs per user, ordered by the moment the electricity went out. */
    private Map<Long, List<PowerOutageLog>> groupLogsByUser(final List<PowerOutageLog> logs) {
        final Map<Long, List<PowerOutageLog>> logsByUser = new LinkedHashMap<>();
        for (final PowerOutageLog log : logs) {
            logsByUser.computeIfAbsent(log.userId(), userId -> new ArrayList<>()).add(log);
        }
        for (final List<PowerOutageLog> userLogs : logsByUser.values()) {
            userLogs.sort(Comparator.comparing(PowerOutageLog::electricityOut));
        }
        return logsByUser;
    }

    /** Evenly spread, deterministic colour per user. */
    private Map<Long, Color> buildUserColors(final List<Long> userIds) {
        final Map<Long, Color> colors = new LinkedHashMap<>();
        final int userCount = userIds.size();
        for (int i = 0; i < userCount; i++) {
            final float hue = (float) (i * 360.0 / userCount) / 360f;
            colors.put(userIds.get(i), Color.getHSBColor(hue, 0.70f, 1.00f));
        }
        return colors;
    }

    /**
     * Splits every outage at midnight boundaries so each segment belongs to a
     * single day (and therefore a single month), then groups the segments by
     * month. The segment's X position is the day of the month.
     */
    private Map<YearMonth, List<Segment>> splitByMonth(final List<PowerOutageLog> logs) {
        final Map<YearMonth, List<Segment>> result = new TreeMap<>();
        final ZoneId timezone = appSettings.getTimezone();

        for (final PowerOutageLog log : logs) {
            final LocalDateTime end = LocalDateTime.ofInstant(log.electricityIn(), timezone);
            LocalDateTime start = LocalDateTime.ofInstant(log.electricityOut(), timezone);
            if (!end.isAfter(start)) {
                continue;
            }
            while (start.isBefore(end)) {
                final LocalDate day = start.toLocalDate();
                final LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
                final LocalDateTime segmentEnd = end.isBefore(dayEnd) ? end : dayEnd;

                final double startHour = start.toLocalTime().toSecondOfDay() / 3600.0;
                final double endHour = segmentEnd.equals(dayEnd)
                        ? 24.0
                        : segmentEnd.toLocalTime().toSecondOfDay() / 3600.0;

                final YearMonth month = YearMonth.from(day);
                result.computeIfAbsent(month, key -> new ArrayList<>())
                        .add(new Segment(day.getDayOfMonth(), startHour, endHour));

                start = segmentEnd;
            }
        }

        return result;
    }

    /** Writes one single-user chart per user per month. */
    private List<Path> writeIndividualCharts(
            final Path outputDir,
            final Map<Long, Map<YearMonth, List<Segment>>> segmentsByUserMonth,
            final Map<Long, String> nicknames,
            final Map<Long, Color> userColors) {
        final List<Path> generated = new ArrayList<>();
        for (final Map.Entry<Long, Map<YearMonth, List<Segment>>> entry : segmentsByUserMonth.entrySet()) {
            final long userId = entry.getKey();
            final String nickname = nicknames.get(userId);

            for (final Map.Entry<YearMonth, List<Segment>> monthEntry : entry.getValue().entrySet()) {
                final YearMonth month = monthEntry.getKey();

                final OutageDataset dataset = new OutageDataset();
                dataset.addSeries(nickname);
                for (final Segment segment : monthEntry.getValue()) {
                    dataset.addSegment(0, segment);
                }
                final JFreeChart chart = buildChart(
                        messageService.get(Message.POWER_OUTAGE_GRAPH_USER_TITLE,
                                Map.of("nickname", nickname, "month", month.toString())),
                        month,
                        dataset,
                        List.of(userColors.get(userId)),
                        false,
                        false);

                final Path outFile = outputDir.resolve(userId + "_" + month + ".png");
                saveChart(chart, outFile, IMAGE_WIDTH, IMAGE_HEIGHT);
                generated.add(outFile);
            }
        }
        return generated;
    }

    /**
     * Writes one wide combined chart per month containing every user. Within a
     * day slot each user gets its own column, ordered by first appearance.
     */
    private List<Path> writeCombinedCharts(
            final Path outputDir,
            final Map<Long, Map<YearMonth, List<Segment>>> segmentsByUserMonth,
            final Map<Long, String> nicknames,
            final Map<Long, Color> userColors) {

        final List<Long> userIds = new ArrayList<>(segmentsByUserMonth.keySet());
        final List<String> labels = buildSeriesLabels(userIds, nicknames);
        final List<Color> seriesColors = userIds.stream().map(userColors::get).collect(Collectors.toList());
        final int userCount = userIds.size();
        final double slotWidth = DAY_GROUP_FILL / userCount;

        final Set<YearMonth> allMonths = new TreeSet<>();
        for (final Map<YearMonth, List<Segment>> months : segmentsByUserMonth.values()) {
            allMonths.addAll(months.keySet());
        }

        final List<Path> generated = new ArrayList<>();
        for (final YearMonth month : allMonths) {
            final OutageDataset dataset = new OutageDataset();
            for (final String label : labels) {
                dataset.addSeries(label);
            }

            for (int i = 0; i < userCount; i++) {
                final long userId = userIds.get(i);
                final double offset = (i - (userCount - 1) / 2.0) * slotWidth;
                final List<Segment> segments = segmentsByUserMonth.get(userId).getOrDefault(month, List.of());
                for (final Segment segment : segments) {
                    dataset.addSegment(i, new Segment(
                            segment.dayPosition() + offset,
                            segment.startHour(),
                            segment.endHour()));
                }
            }

            final JFreeChart chart = buildChart(
                    messageService.get(Message.POWER_OUTAGE_GRAPH_COMBINED_TITLE,
                            Map.of("month", month.toString())),
                    month, dataset, seriesColors, true, true);

            final Path outFile = outputDir.resolve("combined_" + month + ".png");
            saveChart(chart, outFile, COMBINED_IMAGE_WIDTH, COMBINED_IMAGE_HEIGHT);
            generated.add(outFile);
        }
        return generated;
    }

    /** Legend labels, disambiguated with the user id when nicknames collide. */
    private List<String> buildSeriesLabels(final List<Long> userIds, final Map<Long, String> nicknames) {
        final Set<String> usedLabels = new LinkedHashSet<>();
        final List<String> labels = new ArrayList<>();
        for (final long userId : userIds) {
            String label = nicknames.get(userId);
            if (!usedLabels.add(label)) {
                label = label + " (" + userId + ")";
                usedLabels.add(label);
            }
            labels.add(label);
        }
        return labels;
    }

    /**
     * Builds the X axis: one tick per day of the given month.
     *
     * @param month month whose days are shown
     * @return the configured domain axis
     */
    private NumberAxis createDomainAxis(final YearMonth month) {
        final NumberAxis domainAxis = new NumberAxis(messageService.get(Message.POWER_OUTAGE_GRAPH_DAY_AXIS));
        domainAxis.setRange(0.5, month.lengthOfMonth() + 0.5);
        domainAxis.setTickUnit(new NumberTickUnit(1));
        domainAxis.setNumberFormatOverride(new DecimalFormat("0"));
        domainAxis.setAutoRangeIncludesZero(false);
        styleAxis(domainAxis);
        return domainAxis;
    }

    /**
     * Builds the Y axis: hours of the day from 0 to 24.
     *
     * @return the configured range axis
     */
    private NumberAxis createRangeAxis() {
        final NumberAxis rangeAxis = new NumberAxis(messageService.get(Message.POWER_OUTAGE_GRAPH_HOUR_AXIS));
        rangeAxis.setRange(0.0, 24.0);
        rangeAxis.setInverted(true);
        rangeAxis.setTickUnit(new NumberTickUnit(1));
        rangeAxis.setNumberFormatOverride(new DecimalFormat("00"));
        styleAxis(rangeAxis);
        return rangeAxis;
    }

    /**
     * Applies the dark-theme text and line colors to the given axis.
     *
     * @param axis axis to style
     */
    private void styleAxis(final NumberAxis axis) {
        axis.setLabelPaint(AXIS_TEXT_COLOR);
        axis.setTickLabelPaint(AXIS_TEXT_COLOR);
        axis.setAxisLinePaint(GRIDLINE_COLOR);
        axis.setTickMarkPaint(GRIDLINE_COLOR);
    }

    /**
     * Assembles the chart: coloured renderer, axes, gridlines (individual) or
     * day separators (combined), and an optional bottom legend.
     *
     * @param title         chart title
     * @param month         month the domain axis spans
     * @param dataset       outage segments to draw
     * @param seriesColors  paint per series, indexed by series
     * @param showLegend    whether to render the legend
     * @param daySeparators whether to draw day boundary markers on top of the
     *                      bars instead of centre gridlines
     * @return the built chart
     */
    private JFreeChart buildChart(
            final String title,
            final YearMonth month,
            final OutageDataset dataset,
            final List<Color> seriesColors,
            final boolean showLegend,
            final boolean daySeparators) {
        final OutageRenderer renderer = new OutageRenderer();
        for (int i = 0; i < seriesColors.size(); i++) {
            renderer.setSeriesPaint(i, seriesColors.get(i));
        }
        renderer.setDefaultOutlinePaint(BAR_OUTLINE_COLOR);
        renderer.setDefaultOutlineStroke(new BasicStroke(0.5f));

        final XYPlot plot = new XYPlot(dataset, createDomainAxis(month), createRangeAxis(), renderer);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setBackgroundPaint(PLOT_BACKGROUND);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(GRIDLINE_COLOR);
        plot.setRangeGridlinesVisible(true);

        if (daySeparators) {
            // Combined chart: grey lines on the day boundaries, drawn on top of
            // the bars so every bar sits between two lines.
            plot.setDomainGridlinesVisible(false);
            for (int day = 0; day <= month.lengthOfMonth(); day++) {
                final ValueMarker separator = new ValueMarker(day + 0.5);
                separator.setPaint(DAY_SEPARATOR_COLOR);
                separator.setStroke(new BasicStroke(1.0f));
                separator.setLabel(null);
                plot.addDomainMarker(separator, Layer.FOREGROUND);
            }
        } else {
            // Individual chart: one grey guide line down the centre of each day,
            // drawn behind the bars.
            plot.setDomainGridlinePaint(GRIDLINE_COLOR);
            plot.setDomainGridlineStroke(new BasicStroke(1.0f));
            plot.setDomainGridlinesVisible(true);
        }

        final JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, showLegend);
        chart.setBackgroundPaint(CHART_BACKGROUND);
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(TITLE_TEXT_COLOR);
        }
        if (showLegend && chart.getLegend() != null) {
            chart.getLegend().setPosition(RectangleEdge.BOTTOM);
            chart.getLegend().setBackgroundPaint(PLOT_BACKGROUND);
            chart.getLegend().setItemPaint(AXIS_TEXT_COLOR);
            chart.getLegend().setFrame(new BlockBorder(GRIDLINE_COLOR));
        }
        return chart;
    }

    /**
     * Writes the chart as a PNG, logging instead of propagating IO failures.
     *
     * @param chart   chart to write
     * @param outFile destination file
     * @param width   image width in pixels
     * @param height  image height in pixels
     */
    private void saveChart(final JFreeChart chart, final Path outFile, final int width, final int height) {
        try {
            ChartUtils.saveChartAsPNG(outFile.toFile(), chart, width, height);
        } catch (final IOException exception) {
            LOGGER.error("Failed to write graph '{}'", outFile, exception);
        }
    }

    /**
     * An outage clipped to a single day, expressed in fractional hours.
     * {@code dayPosition} is the X value: the day of the month for single-user
     * charts, or that plus a per-user column offset for combined charts.
     */
    private record Segment(double dayPosition, double startHour, double endHour) {
    }

    /**
     * Multi-series in-memory dataset: one series per user, each holding that
     * user's outage segments for a month. X is the (possibly offset) day
     * position, Y is the start hour; the end hour is exposed separately for the
     * renderer.
     */
    private static final class OutageDataset extends AbstractXYDataset {
        private static final long serialVersionUID = 1L;

        private final List<String> seriesKeys = new ArrayList<>();
        private final List<List<Segment>> seriesSegments = new ArrayList<>();

        /**
         * Appends an empty series.
         *
         * @param seriesKey legend key of the series
         */
        void addSeries(final String seriesKey) {
            seriesKeys.add(seriesKey);
            seriesSegments.add(new ArrayList<>());
        }

        /**
         * Appends a segment to a series.
         *
         * @param series  series index
         * @param segment segment to add
         */
        void addSegment(final int series, final Segment segment) {
            seriesSegments.get(series).add(segment);
        }

        /**
         * @param series series index
         * @param item   item index
         * @return the end hour of the segment, which is not part of the XY pair
         */
        double getEndHour(final int series, final int item) {
            return seriesSegments.get(series).get(item).endHour();
        }

        @Override
        public int getSeriesCount() {
            return seriesKeys.size();
        }

        @Override
        public Comparable<String> getSeriesKey(final int series) {
            return seriesKeys.get(series);
        }

        @Override
        public int getItemCount(final int series) {
            return seriesSegments.get(series).size();
        }

        @Override
        public Number getX(final int series, final int item) {
            return seriesSegments.get(series).get(item).dayPosition();
        }

        @Override
        public Number getY(final int series, final int item) {
            return seriesSegments.get(series).get(item).startHour();
        }
    }

    /**
     * Draws each item as a floating vertical bar spanning its start and end
     * hour, centred on its X position. The bar width shrinks as the number of
     * series grows so users line up side by side within each day slot.
     */
    private static final class OutageRenderer extends AbstractXYItemRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public void drawItem(
                final Graphics2D g2,
                final XYItemRendererState state,
                final Rectangle2D dataArea,
                final PlotRenderingInfo info,
                final XYPlot plot,
                final ValueAxis domainAxis,
                final ValueAxis rangeAxis,
                final XYDataset dataset,
                final int series,
                final int item,
                final CrosshairState crosshairState,
                final int pass) {

            final OutageDataset outageDataset = (OutageDataset) dataset;
            final double halfWidth = dataset.getSeriesCount() == 1
                    ? SINGLE_BAR_HALF_WIDTH
                    : 0.4 * DAY_GROUP_FILL / dataset.getSeriesCount();
            final double position = dataset.getXValue(series, item);
            final double startHour = dataset.getYValue(series, item);
            final double endHour = outageDataset.getEndHour(series, item);

            final RectangleEdge domainEdge = plot.getDomainAxisEdge();
            final RectangleEdge rangeEdge = plot.getRangeAxisEdge();

            final double x1 = domainAxis.valueToJava2D(position - halfWidth, dataArea, domainEdge);
            final double x2 = domainAxis.valueToJava2D(position + halfWidth, dataArea, domainEdge);
            final double y1 = rangeAxis.valueToJava2D(startHour, dataArea, rangeEdge);
            final double y2 = rangeAxis.valueToJava2D(endHour, dataArea, rangeEdge);

            final double x = Math.min(x1, x2);
            final double y = Math.min(y1, y2);
            final double width = Math.abs(x2 - x1);
            final double height = Math.max(Math.abs(y2 - y1), 2.0);

            final Rectangle2D bar = new Rectangle2D.Double(x, y, width, height);
            g2.setPaint(getItemPaint(series, item));
            g2.fill(bar);
            g2.setPaint(getItemOutlinePaint(series, item));
            g2.setStroke(getItemOutlineStroke(series, item));
            g2.draw(bar);
        }
    }
}
