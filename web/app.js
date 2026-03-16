class CrossPlotApp {
    constructor() {
        this.canvas = document.getElementById('canvas');
        this.ctx = this.canvas.getContext('2d');
        this.tooltip = document.getElementById('tooltip');

        this.points = [];
        this.curveData = null;
        this.isDragging = false;
        this.dragIndex = -1;
        this.hoverIndex = -1;

        this.settings = {
            tension: 1.0,
            samples: 50,
            showCurve: true,
            showControlPolygon: true,
            showTangents: false,
            showGrid: true,
            antialiasing: true
        };

        this.colors = {
            curve: '#00d4ff',
            controlPolygon: '#ff6b6b',
            points: '#ffd93d',
            pointHover: '#ffffff',
            tangents: '#6bcb77',
            grid: 'rgba(255,255,255,0.05)',
            gridMajor: 'rgba(255,255,255,0.1)'
        };

        this.pointRadius = 8;
        this.init();
    }

    init() {
        this.setupCanvas();
        this.setupEventListeners();
        this.draw();
    }

    setupCanvas() {
        const container = this.canvas.parentElement;
        const rect = container.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;

        this.canvas.width = (rect.width - 20) * dpr;
        this.canvas.height = (rect.height - 20) * dpr;
        this.canvas.style.width = (rect.width - 20) + 'px';
        this.canvas.style.height = (rect.height - 20) + 'px';

        this.ctx.scale(dpr, dpr);
        this.width = rect.width - 20;
        this.height = rect.height - 20;
    }

    setupEventListeners() {
        window.addEventListener('resize', () => {
            this.setupCanvas();
            this.draw();
        });

        this.canvas.addEventListener('mousedown', (e) => this.onMouseDown(e));
        this.canvas.addEventListener('mousemove', (e) => this.onMouseMove(e));
        this.canvas.addEventListener('mouseup', (e) => this.onMouseUp(e));
        this.canvas.addEventListener('mouseleave', (e) => this.onMouseLeave(e));
        this.canvas.addEventListener('dblclick', (e) => this.onDoubleClick(e));

        this.canvas.addEventListener('touchstart', (e) => this.onTouchStart(e));
        this.canvas.addEventListener('touchmove', (e) => this.onTouchMove(e));
        this.canvas.addEventListener('touchend', (e) => this.onTouchEnd(e));

        document.getElementById('tensionSlider').addEventListener('input', (e) => {
            this.settings.tension = parseFloat(e.target.value);
            document.getElementById('tensionValue').textContent = this.settings.tension.toFixed(2);
            this.updateCurve();
        });

        document.getElementById('samplesInput').addEventListener('change', (e) => {
            this.settings.samples = parseInt(e.target.value);
            this.updateCurve();
        });

        const checkboxes = ['showCurve', 'showControlPolygon', 'showTangents', 'showGrid', 'antialiasing'];
        checkboxes.forEach(id => {
            document.getElementById(id).addEventListener('change', (e) => {
                this.settings[id] = e.target.checked;
                this.draw();
            });
        });
    }

    getMousePos(e) {
        const rect = this.canvas.getBoundingClientRect();
        return {
            x: e.clientX - rect.left,
            y: e.clientY - rect.top
        };
    }

    findPointAtPosition(x, y) {
        for (let i = 0; i < this.points.length; i++) {
            const p = this.points[i];
            const dist = Math.sqrt((p.x - x) ** 2 + (p.y - y) ** 2);
            if (dist <= this.pointRadius + 5) {
                return i;
            }
        }
        return -1;
    }

    onMouseDown(e) {
        const pos = this.getMousePos(e);
        const index = this.findPointAtPosition(pos.x, pos.y);

        if (index >= 0) {
            this.isDragging = true;
            this.dragIndex = index;
            this.canvas.style.cursor = 'grabbing';
        } else {
            this.points.push({ x: pos.x, y: pos.y });
            this.updateCurve();
        }
    }

    onMouseMove(e) {
        const pos = this.getMousePos(e);

        if (this.isDragging && this.dragIndex >= 0) {
            this.points[this.dragIndex] = { x: pos.x, y: pos.y };
            this.updateCurve();
        } else {
            const index = this.findPointAtPosition(pos.x, pos.y);

            if (index !== this.hoverIndex) {
                this.hoverIndex = index;
                this.canvas.style.cursor = index >= 0 ? 'grab' : 'crosshair';
                this.draw();
            }

            if (index >= 0) {
                const p = this.points[index];
                this.showTooltip(e.clientX, e.clientY, `Точка ${index + 1}: (${p.x.toFixed(1)}, ${p.y.toFixed(1)})`);
            } else {
                this.hideTooltip();
            }
        }
    }

    onMouseUp(e) {
        if (this.isDragging) {
            this.isDragging = false;
            this.dragIndex = -1;
            this.canvas.style.cursor = 'crosshair';
        }
    }

    onMouseLeave(e) {
        this.isDragging = false;
        this.dragIndex = -1;
        this.hoverIndex = -1;
        this.hideTooltip();
        this.draw();
    }

    onDoubleClick(e) {
        const pos = this.getMousePos(e);
        const index = this.findPointAtPosition(pos.x, pos.y);

        if (index >= 0) {
            this.points.splice(index, 1);
            this.updateCurve();
        }
    }

    onTouchStart(e) {
        e.preventDefault();
        const touch = e.touches[0];
        const mouseEvent = new MouseEvent('mousedown', {
            clientX: touch.clientX,
            clientY: touch.clientY
        });
        this.onMouseDown(mouseEvent);
    }

    onTouchMove(e) {
        e.preventDefault();
        const touch = e.touches[0];
        const mouseEvent = new MouseEvent('mousemove', {
            clientX: touch.clientX,
            clientY: touch.clientY
        });
        this.onMouseMove(mouseEvent);
    }

    onTouchEnd(e) {
        e.preventDefault();
        this.onMouseUp(null);
    }

    showTooltip(x, y, text) {
        this.tooltip.textContent = text;
        this.tooltip.style.display = 'block';
        this.tooltip.style.left = (x + 15) + 'px';
        this.tooltip.style.top = (y - 10) + 'px';
    }

    hideTooltip() {
        this.tooltip.style.display = 'none';
    }

    async updateCurve() {
        document.getElementById('pointCount').textContent = this.points.length;
        document.getElementById('segmentCount').textContent = Math.max(0, this.points.length - 1);

        if (this.points.length < 2) {
            this.curveData = null;
            this.draw();
            return;
        }

        try {
            await fetch('/api/tension', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ tension: this.settings.tension })
            });

            const response = await fetch('/api/curve', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(this.points)
            });

            if (response.ok) {
                this.curveData = await response.json();
                this.draw();
            }
        } catch (error) {
            console.error('Грешка при комуникация със сървъра:', error);
            this.curveData = this.computeCurveLocally();
            this.draw();
        }
    }

    computeCurveLocally() {
        if (this.points.length < 2) return null;

        const tension = this.settings.tension;
        const samples = this.settings.samples;

        const tangents = [];
        for (let i = 0; i < this.points.length; i++) {
            let tangent;
            if (i === 0) {
                tangent = this.normalize(this.subtract(this.points[1], this.points[0]));
            } else if (i === this.points.length - 1) {
                tangent = this.normalize(this.subtract(this.points[i], this.points[i-1]));
            } else {
                tangent = this.normalize(this.subtract(this.points[i+1], this.points[i-1]));
            }
            tangents.push(tangent);
        }

        const alphas = [];
        for (let i = 0; i < this.points.length; i++) {
            let alpha;
            if (i === 0) {
                alpha = this.distance(this.points[0], this.points[1]) * tension;
            } else if (i === this.points.length - 1) {
                alpha = this.distance(this.points[i-1], this.points[i]) * tension;
            } else {
                const d1 = this.distance(this.points[i], this.points[i-1]);
                const d2 = this.distance(this.points[i], this.points[i+1]);
                alpha = (d1 + d2) / 2 * tension;
            }
            alphas.push(alpha);
        }

        const controlPolygons = [];
        const curvePoints = [];

        for (let i = 0; i < this.points.length - 1; i++) {
            const p0 = this.points[i];
            const p3 = this.points[i + 1];

            const p1 = {
                x: p0.x + tangents[i].x * alphas[i] / 3,
                y: p0.y + tangents[i].y * alphas[i] / 3
            };

            const p2 = {
                x: p3.x - tangents[i+1].x * alphas[i+1] / 3,
                y: p3.y - tangents[i+1].y * alphas[i+1] / 3
            };

            controlPolygons.push([p0, p1, p2, p3]);

            for (let j = 0; j <= samples; j++) {
                const t = j / samples;
                const point = this.deCasteljau([p0, p1, p2, p3], t);
                if (i === 0 || j > 0) {
                    curvePoints.push(point);
                }
            }
        }

        return {
            curve: curvePoints,
            controlPolygons: controlPolygons,
            tangents: tangents.map((t, i) => ({ point: this.points[i], tangent: t, alpha: alphas[i] }))
        };
    }

    subtract(a, b) {
        return { x: a.x - b.x, y: a.y - b.y };
    }

    normalize(v) {
        const len = Math.sqrt(v.x * v.x + v.y * v.y);
        if (len < 1e-10) return { x: 1, y: 0 };
        return { x: v.x / len, y: v.y / len };
    }

    distance(a, b) {
        return Math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2);
    }

    deCasteljau(controlPoints, t) {
        let points = [...controlPoints];

        while (points.length > 1) {
            const newPoints = [];
            for (let i = 0; i < points.length - 1; i++) {
                newPoints.push({
                    x: points[i].x * (1 - t) + points[i + 1].x * t,
                    y: points[i].y * (1 - t) + points[i + 1].y * t
                });
            }
            points = newPoints;
        }

        return points[0];
    }

    draw() {
        const ctx = this.ctx;
        ctx.imageSmoothingEnabled = this.settings.antialiasing;

        ctx.fillStyle = '#0a0a14';
        ctx.fillRect(0, 0, this.width, this.height);

        if (this.settings.showGrid) {
            this.drawGrid();
        }

        if (this.curveData) {
            if (this.settings.showControlPolygon && this.curveData.controlPolygons) {
                this.drawControlPolygons(this.curveData.controlPolygons);
            }

            if (this.settings.showTangents && this.curveData.tangents) {
                this.drawTangents(this.curveData.tangents);
            }

            if (this.settings.showCurve && this.curveData.curve) {
                this.drawCurve(this.curveData.curve);
            }
        }

        this.drawPoints();
    }

    drawGrid() {
        const ctx = this.ctx;
        const gridSize = 50;

        ctx.strokeStyle = this.colors.grid;
        ctx.lineWidth = 1;

        for (let x = 0; x < this.width; x += gridSize) {
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, this.height);
            ctx.stroke();
        }

        for (let y = 0; y < this.height; y += gridSize) {
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(this.width, y);
            ctx.stroke();
        }

        ctx.strokeStyle = this.colors.gridMajor;

        for (let x = 0; x < this.width; x += 100) {
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, this.height);
            ctx.stroke();
        }

        for (let y = 0; y < this.height; y += 100) {
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(this.width, y);
            ctx.stroke();
        }
    }

    drawCurve(curvePoints) {
        if (curvePoints.length < 2) return;

        const ctx = this.ctx;

        ctx.shadowColor = this.colors.curve;
        ctx.shadowBlur = 10;
        ctx.strokeStyle = this.colors.curve;
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';

        ctx.beginPath();
        ctx.moveTo(curvePoints[0].x, curvePoints[0].y);

        for (let i = 1; i < curvePoints.length; i++) {
            ctx.lineTo(curvePoints[i].x, curvePoints[i].y);
        }

        ctx.stroke();
        ctx.shadowBlur = 0;
    }

    drawControlPolygons(controlPolygons) {
        const ctx = this.ctx;

        ctx.strokeStyle = this.colors.controlPolygon;
        ctx.lineWidth = 1;
        ctx.setLineDash([5, 5]);

        for (const polygon of controlPolygons) {
            ctx.beginPath();
            ctx.moveTo(polygon[0].x, polygon[0].y);

            for (let i = 1; i < polygon.length; i++) {
                ctx.lineTo(polygon[i].x, polygon[i].y);
            }

            ctx.stroke();

            ctx.fillStyle = this.colors.controlPolygon;
            for (let i = 1; i < polygon.length - 1; i++) {
                ctx.beginPath();
                ctx.arc(polygon[i].x, polygon[i].y, 4, 0, Math.PI * 2);
                ctx.fill();
            }
        }

        ctx.setLineDash([]);
    }

    drawTangents(tangentData) {
        const ctx = this.ctx;
        const tangentLength = 50;

        ctx.strokeStyle = this.colors.tangents;
        ctx.lineWidth = 2;

        if (tangentData[0] && tangentData[0].point) {
            for (const data of tangentData) {
                const p = data.point;
                const t = data.tangent;
                const len = Math.min(tangentLength, data.alpha / 3);

                ctx.beginPath();
                ctx.moveTo(p.x - t.x * len, p.y - t.y * len);
                ctx.lineTo(p.x + t.x * len, p.y + t.y * len);
                ctx.stroke();

                this.drawArrow(p.x + t.x * len, p.y + t.y * len, t.x, t.y);
            }
        }
    }

    drawArrow(x, y, dx, dy) {
        const ctx = this.ctx;
        const headLen = 10;
        const angle = Math.atan2(dy, dx);

        ctx.beginPath();
        ctx.moveTo(x, y);
        ctx.lineTo(x - headLen * Math.cos(angle - Math.PI / 6), y - headLen * Math.sin(angle - Math.PI / 6));
        ctx.moveTo(x, y);
        ctx.lineTo(x - headLen * Math.cos(angle + Math.PI / 6), y - headLen * Math.sin(angle + Math.PI / 6));
        ctx.stroke();
    }

    drawPoints() {
        const ctx = this.ctx;

        for (let i = 0; i < this.points.length; i++) {
            const p = this.points[i];
            const isHovered = i === this.hoverIndex;
            const isDragged = i === this.dragIndex;

            if (isHovered || isDragged) {
                ctx.shadowColor = this.colors.pointHover;
                ctx.shadowBlur = 15;
            }

            ctx.fillStyle = isHovered || isDragged ? this.colors.pointHover : this.colors.points;
            ctx.beginPath();
            ctx.arc(p.x, p.y, this.pointRadius + (isHovered ? 3 : 0), 0, Math.PI * 2);
            ctx.fill();

            ctx.fillStyle = '#0a0a14';
            ctx.beginPath();
            ctx.arc(p.x, p.y, this.pointRadius - 3, 0, Math.PI * 2);
            ctx.fill();

            ctx.fillStyle = this.colors.points;
            ctx.font = 'bold 10px Arial';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText((i + 1).toString(), p.x, p.y);

            ctx.shadowBlur = 0;
        }
    }
}

function clearCanvas() {
    app.points = [];
    app.curveData = null;
    app.updateCurve();
}

function addDemoPoints() {
    const centerX = app.width / 2;
    const centerY = app.height / 2;
    const radius = Math.min(app.width, app.height) * 0.35;

    app.points = [
        { x: centerX - radius, y: centerY },
        { x: centerX - radius * 0.5, y: centerY - radius * 0.8 },
        { x: centerX + radius * 0.5, y: centerY - radius * 0.6 },
        { x: centerX + radius, y: centerY },
        { x: centerX + radius * 0.5, y: centerY + radius * 0.8 },
        { x: centerX - radius * 0.3, y: centerY + radius * 0.5 }
    ];

    app.updateCurve();
}

let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new CrossPlotApp();
});
