package MainPackage;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JPanel;

public class GraphicsDisplay extends JPanel {

    private Double[][] graphicsData;

    private ArrayList<double[][]> undoHistory;

    private boolean showAxis = true;
    private boolean showMarkers = true;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    private double initialMinX;
    private double initialMaxX;
    private double initialMinY;
    private double initialMaxY;

    private final BasicStroke graphicsStroke;
    private final BasicStroke axisStroke;
    private final BasicStroke markerStroke;
    private BasicStroke gridStroke;

    private Point2D.Double mousePoint = null;
    private Double[] highlightedPoint = null;

    private Point selectionStart = null;
    private Rectangle2D.Double selectionRect = null;
    private Font axisFont;

    private double scale = 1.0;

    public GraphicsDisplay() {
        setBackground(Color.WHITE);
        undoHistory = new ArrayList<double[][]>(5);

        selectionRect = new Rectangle2D.Double();
        graphicsStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[]{15, 5, 2, 5, 7, 5, 2, 5, 15, 5}, 0.0f);
        axisStroke = new BasicStroke(2.0f);
        markerStroke = new BasicStroke(1.5f);
        gridStroke = new BasicStroke(0.5f);
        addMouseMotionListener(new MouseMotionHandler());
        addMouseListener(new MouseClickHandler());

        axisFont = new Font("Serif", Font.BOLD, 18);
    }

    public void showGraphics(Double[][] graphicsData) {
        if (graphicsData == null || graphicsData.length == 0) {
            System.out.println("Нет данных для отображения");
            return;
        }
        this.graphicsData = graphicsData;

        initialMinX = graphicsData[0][0];
        initialMaxX = graphicsData[graphicsData.length - 1][0];
        initialMinY = graphicsData[0][1];
        initialMaxY = initialMinY;

        for (int i = 1; i < graphicsData.length; i++) {
            if (graphicsData[i][1] < initialMinY) initialMinY = graphicsData[i][1];
            if (graphicsData[i][1] > initialMaxY) initialMaxY = graphicsData[i][1];
        }

        minX = initialMinX;
        maxX = initialMaxX;
        minY = initialMinY;
        maxY = initialMaxY;

        repaint();
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    public void zoomToRegion(double x1, double y1, double x2, double y2) {
        minX = x1;
        maxY = y2;
        maxX = x2;
        minY = y1;

        double scaleX = getSize().getWidth() / (maxX - minX);
        double scaleY = getSize().getHeight() / (maxY - minY);
        scale = Math.min(scaleX, scaleY);

        repaint();
    }

    private void resetZoom() {
        minX = initialMinX;
        maxX = initialMaxX;
        minY = initialMinY;
        maxY = initialMaxY;

        repaint();
    }

    protected double[] translatePointToXY(int x, int y) {
        return new double[]{minX + x / scale, maxY - y / scale};
    }

    private class MouseMotionHandler extends java.awt.event.MouseMotionAdapter {
        @Override
        public void mouseMoved(java.awt.event.MouseEvent e) {
            mousePoint = new Point2D.Double(e.getX(), e.getY());
            highlightedPoint = findPointNearCursor(mousePoint);
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectionStart != null) {
                selectionRect.setFrameFromDiagonal(selectionStart, e.getPoint());
                repaint();
            }
        }
    }

    private class MouseClickHandler extends java.awt.event.MouseAdapter {
        @Override
        public void mousePressed(MouseEvent ev) {
            if (ev.getButton() == MouseEvent.BUTTON1) {
                selectionStart = ev.getPoint();
                selectionRect.setFrameFromDiagonal(selectionStart, selectionStart);
            } else if (ev.getButton() == MouseEvent.BUTTON3) {
                resetZoom();
            }
        }

        @Override
        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() != MouseEvent.BUTTON1 || selectionStart == null) {
                return;
            }

            Point selectionEndPoint = ev.getPoint();

            double[] startCoords = translatePointToXY((int) selectionStart.getX(), (int) selectionStart.getY());
            double[] endCoords = translatePointToXY((int) selectionEndPoint.getX(), (int) selectionEndPoint.getY());

            double minX = Math.min(startCoords[0], endCoords[0]);
            double maxX = Math.max(startCoords[0], endCoords[0]);
            double minY = Math.min(startCoords[1], endCoords[1]);
            double maxY = Math.max(startCoords[1], endCoords[1]);

            zoomToRegion(minX, minY, maxX, maxY);

            selectionStart = null;

            repaint();
        }
    }

    private Double[] findPointNearCursor(Point2D.Double mousePoint) {
        if (graphicsData == null) {
            return null;
        }

        double maxDistance = 10;
        for (Double[] point : graphicsData) {
            Point2D.Double graphPoint = xyToPoint(point[0], point[1]);
            if (mousePoint.distance(graphPoint) < maxDistance) {
                return point;
            }
        }
        return null;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D canvas = (Graphics2D) g;

        if (graphicsData == null || graphicsData.length == 0) return;

        double scaleX = getSize().getWidth() / (maxX - minX);
        double scaleY = getSize().getHeight() / (maxY - minY);
        scale = Math.min(scaleX, scaleY);

        canvas.setStroke(new BasicStroke(2));
        canvas.setColor(Color.BLACK);
        if (showAxis) paintAxis(canvas);
        if (showDivisions) paintDivisions(canvas);
        paintGraphics(canvas);
        if (showMarkers) paintMarkers(canvas);

        if (selectionStart != null) {
            canvas.setColor(Color.BLUE);
            canvas.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[]{5, 5}, 0.0f));
            canvas.draw(selectionRect);
        }
    }

    protected void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(graphicsStroke);
        canvas.setColor(Color.RED);

        GeneralPath graphics = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
            Point2D.Double point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            if (i > 0) {
                graphics.lineTo(point.getX(), point.getY());
            } else {
                graphics.moveTo(point.getX(), point.getY());
            }
        }

        canvas.draw(graphics);
    }

    private boolean showDivisions = false;

    public void setShowDivisions(boolean showDivisions) {
        this.showDivisions = showDivisions;
        repaint();
    }

    protected void paintDivisions(Graphics2D canvas) {
        canvas.setStroke(axisStroke);
        canvas.setColor(Color.BLACK);

        double stepX = (maxX - minX) / 100.0;
        double stepY = (maxY - minY) / 100.0;

        for (int i = 0; i <= 100; i++) {
            double x = minX + i * stepX;
            Point2D.Double bottom = xyToPoint(x, 0);
            double length = (i % 5 == 0) ? 10 : 5;
            canvas.draw(new Line2D.Double(bottom.getX(), bottom.getY() - length, bottom.getX(), bottom.getY() + length));
        }

        for (int i = 0; i <= 100; i++) {
            double y = minY + i * stepY;
            Point2D.Double left = xyToPoint(0, y);
            double length = (i % 5 == 0) ? 10 : 5;
            canvas.draw(new Line2D.Double(left.getX() - length, left.getY(), left.getX() + length, left.getY()));
        }
    }

    private boolean isNearInteger(double value) {
        double fractionalPart = Math.abs(value - Math.round(value));
        return fractionalPart <= 0.1;
    }

    protected void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(markerStroke);

        for (Double[] point : graphicsData) {
            Point2D.Double center = xyToPoint(point[0], point[1]);
            boolean isNearCursor = highlightedPoint != null && highlightedPoint == point;
            canvas.setColor(isNearCursor ? Color.RED : Color.BLUE);
            boolean isNearInteger = isNearInteger(point[1]);
            canvas.setColor(isNearInteger ? Color.GREEN : Color.BLUE);

            double size = 5.5;
            canvas.draw(new Rectangle2D.Double(center.getX() - size, center.getY() - size, size * 2, size * 2));
            canvas.draw(new Line2D.Double(center.getX() - size, center.getY() - size, center.getX() + size, center.getY() + size));
            canvas.draw(new Line2D.Double(center.getX() - size, center.getY() + size, center.getX() + size, center.getY() - size));

            if (isNearCursor) {
                canvas.setFont(new Font("Serif", Font.PLAIN, 12));
                canvas.setColor(Color.BLACK);
                String label = String.format("(%.2f, %.2f)", point[0], point[1]);
                canvas.drawString(label, (float) center.getX() + 10, (float) center.getY() - 10);
            }
        }
    }

    protected void paintAxis(Graphics2D canvas) {
        canvas.setStroke(axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setFont(axisFont);
        FontRenderContext context = canvas.getFontRenderContext();

        // Определяем границы по осям
        double axisOffset = 0; // Увеличьте смещение для оси Y
        // Рисуем ось Y (вертикальная линия)
        canvas.draw(new Line2D.Double(xyToPoint(axisOffset, 1), xyToPoint(axisOffset, -1))); // Вертикальная ось на 50px вправо

        // Рисуем стрелки и метку для оси Y
        Point2D.Double lineEnd = xyToPoint(axisOffset, 1000); // Конечная точка для верхней стрелки
        canvas.draw(new Line2D.Double(lineEnd.getX(), lineEnd.getY(), lineEnd.getX() - 5, lineEnd.getY() + 10));
        canvas.draw(new Line2D.Double(lineEnd.getX(), lineEnd.getY(), lineEnd.getX() + 5, lineEnd.getY() + 10));

        // Подпись оси Y
        Rectangle2D bounds = axisFont.getStringBounds("Y", context);
        Point2D.Double labelPos = xyToPoint(axisOffset, 1000);
        canvas.drawString("Y", (float) labelPos.getX() + 10, (float) (labelPos.getY() - bounds.getY()));

        // Рисуем ось X (горизонтальная линия)
        canvas.draw(new Line2D.Double(xyToPoint(-1000, 0), xyToPoint(1000, 0)));

        // Рисуем стрелки и метку для оси X
        Point2D.Double lineEndX = xyToPoint(1000, 0);
        canvas.draw(new Line2D.Double(lineEndX.getX(), lineEndX.getY(), lineEndX.getX() - 10, lineEndX.getY() - 5));
        canvas.draw(new Line2D.Double(lineEndX.getX(), lineEndX.getY(), lineEndX.getX() - 10, lineEndX.getY() + 5));

        // Подпись оси X
        bounds = axisFont.getStringBounds("X", context);
        labelPos = xyToPoint(1000, 0);
        canvas.drawString("X", (float) (labelPos.getX() - bounds.getWidth() - 10), (float) (labelPos.getY() + bounds.getHeight()));
    }

    protected Point2D.Double xyToPoint(double x, double y) {
        double deltaX = (x - minX) + 0.3;
        double deltaY = maxY - y - 0.3;
        return new Point2D.Double(deltaX * scale, deltaY * scale);
    }
}