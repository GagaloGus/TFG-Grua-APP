import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SupabaseService } from '@services/supabase.service';
import { Servicio, Vehiculo, Usuario, Tablas, Disponibilidad, Estado, Roles } from '@services/tablas.supabase';

@Component({
  selector: 'app-admin-dashboard',
  imports: [FormsModule],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard {
  constructor(private supabaseService: SupabaseService, private router: Router) { }

  // ── SIGNALS ──────────────────────────────────────────────────────────────────
  finishedLoading = signal(false);
  servicios       = signal<Servicio[]>([]);
  vehiculos       = signal<Vehiculo[]>([]);
  usuarios        = signal<Usuario[]>([]);
  successMsg      = signal('');
  errorMsg        = signal('');
  modalErrorMsg   = signal('');

  // ── KPIs ──────────────────────────────────────────────────────────────────────
  contarTrabajActivos        = computed(() => this.usuarios().filter(u => u.disponibilidad == Disponibilidad.EN_SERVICIO).length);
  contarServiciosActivos     = computed(() => this.servicios().filter(s => s.estado == Estado.EN_CURSO).length);
  contarServiciosCompletados = computed(() => this.servicios().filter(s => s.estado == Estado.TERMINADO).length);
  contarVehiculosDisponibles = computed(() => this.vehiculos().filter(v => v.disponible).length);
  totalServicios             = computed(() => this.servicios().length);
  serviciosHoy = computed(() => {
    const hoy = new Date();
    return this.servicios().filter(s => {
      if (!s.fecha) return false;
      const dateServ = new Date(s.fecha);
      return dateServ.getDate()     === hoy.getDate()     &&
             dateServ.getMonth()    === hoy.getMonth()    &&
             dateServ.getFullYear() === hoy.getFullYear();
    });
  });

  // ── DONUT ─────────────────────────────────────────────────────────────────────
  donutSegmentosServicios = computed(() => {
    const counts = [
      { value: this.serviciosHoy().filter(s => s.estado === 'En curso' && s.fecha).length,    color: '#3b82f6', label: 'En curso' },
      { value: this.serviciosHoy().filter(s => s.estado === 'Terminado').length,   color: '#10b981', label: 'Terminado' },
      { value: this.serviciosHoy().filter(s => s.estado === 'Sin empezar').length, color: '#f59e0b', label: 'Sin empezar' },
      { value: this.serviciosHoy().filter(s => s.estado === 'Cancelado').length,   color: '#9ca3af', label: 'Cancelado' },
    ];
    const total = counts.reduce((s, c) => s + c.value, 0) || 1;
    const circ = 2 * Math.PI * 60;
    let offset = 0;
    return counts.map(seg => {
      const dash = (seg.value / total) * circ;
      const gap  = circ - dash;
      const result = { ...seg, dash, gap, offset, pct: Math.round((seg.value / total) * 100) };
      offset += dash;
      return result;
    });
  });

  // ── BARRAS CONDUCTORES ────────────────────────────────────────────────────────
  disponibilidadTrabajadores = computed(() => {
    const disponible = this.usuarios().filter(u => u.disponibilidad === 'Disponible').length;
    const enServicio = this.usuarios().filter(u => u.disponibilidad === 'En servicio').length;
    const inactivo   = this.usuarios().filter(u => u.disponibilidad === 'Inactivo').length;
    const max = Math.max(disponible, enServicio, inactivo, 1);
    return [
      { label: 'Disponible',  count: disponible, pct: (disponible / max) * 100, color: '#10b981' },
      { label: 'En servicio', count: enServicio,  pct: (enServicio  / max) * 100, color: '#3b82f6' },
      { label: 'Inactivo',    count: inactivo,    pct: (inactivo    / max) * 100, color: '#9ca3af' },
    ];
  });

  // ── BARRAS VEHÍCULOS ──────────────────────────────────────────────────────────
  disponibilidadVehiculos = computed(() => {
    const disponible = this.vehiculos().filter(v => v.disponible && v.activo).length;
    const ocupada    = this.vehiculos().filter(v => !v.disponible && v.activo).length;
    const inactiva   = this.vehiculos().filter(v => !v.activo).length;
    const max = Math.max(disponible, ocupada, inactiva, 1);
    return [
      { label: 'Disponible', count: disponible, pct: (disponible / max) * 100, color: '#10b981' },
      { label: 'Ocupada',    count: ocupada,    pct: (ocupada    / max) * 100, color: '#f59e0b' },
      { label: 'Inactiva',   count: inactiva,   pct: (inactiva   / max) * 100, color: '#9ca3af' },
    ];
  });


  // ── GRÁFICO DE LÍNEAS ── últimos X días ───────────────────────────────────────
  lineChartCantidadDias = signal(7)

  // Step automatico segun el rango
  lineChartStep = computed(() => {
    const dias = this.lineChartCantidadDias();
    if (dias <= 14) return 1;    // cada dia
    if (dias <= 60) return 7;    // semanal
    if (dias <= 180) return 14;  // quincenal
    return 30;                   // mensual
  });

  lineChartDias = computed(() => {
    const totalDias = this.lineChartCantidadDias();
    const step = this.lineChartStep();
    const hoy = new Date();

    // Generamos solo los puntos que vamos a mostrar
    // Siempre incluye hoy como último punto
    const puntos: number[] = [];
    for (let offset = totalDias - 1; offset >= 0; offset -= step) {
      puntos.push(offset);
    }

    return puntos.map(offset => {
      const d = new Date(hoy);
      d.setDate(hoy.getDate() - offset);

      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      const key = `${yyyy}-${mm}-${dd}`;

      // Etiqueta adaptada al step: con mes si el step es grande
      let label = ""

      if (step <= 1)
        label = d.toLocaleDateString('es-ES', { weekday: 'short', day: 'numeric' });    // cada dia
      else if (step <= 14)
        label = d.toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });    // semanal
      else
        label = d.toLocaleDateString('es-ES', { month: 'short', year: '2-digit' });    // anual

      // Acumulamos desde este punto hasta el siguiente step
      // (agrupa los días intermedios que no se muestran)
      const hasta = new Date(d);
      hasta.setDate(d.getDate() + step - 1);

      const delRango = this.servicios().filter(s => {
        if (!s.fecha) return false;
        const f = s.fecha.substring(0, 10);
        return f >= key && f <= `${hasta.getFullYear()}-${String(hasta.getMonth() + 1).padStart(2, '0')}-${String(hasta.getDate()).padStart(2, '0')}`;
      });

      const ingresos = delRango.reduce((sum, s) => sum + ((s as any).ingresos ?? 0), 0);
      const gastos = delRango.reduce((sum, s) => sum + (s.costo ?? 0), 0);

      return { label, key, ingresos, gastos, servicios: delRango.length };
    });
  });


  lineChartSvg = computed(() => {
    const dias  = this.lineChartDias();
    const W = 420, H = 140, padX = 8, padTop = 10, padBot = 24;
    const innerH = H - padTop - padBot;

    const maxY = Math.max(...dias.flatMap(d => [d.ingresos, d.gastos]), 1);
    const xStep  = (W - padX * 2) / (dias.length - 1);
    const xOf    = (i: number) => padX + i * xStep;
    const yOf    = (v: number) => padTop + innerH - (v / maxY) * innerH;

    const polyline = (key: 'ingresos' | 'gastos') =>
      dias.map((d, i) => `${xOf(i).toFixed(1)},${yOf(d[key]).toFixed(1)}`).join(' ');

    const toPath = (pts: string) =>
      pts.split(' ').map((p, i) => (i === 0 ? `M${p}` : `L${p}`)).join(' ');

    const ptsI = polyline('ingresos');
    const ptsG = polyline('gastos');
    const baseY = (padTop + innerH).toFixed(1);
    const lastX = xOf(dias.length - 1).toFixed(1);

    const areaI = toPath(ptsI) + ` L${lastX},${baseY} L${padX},${baseY} Z`;
    const areaG = toPath(ptsG) + ` L${lastX},${baseY} L${padX},${baseY} Z`;

    // etiquetas Y (3 niveles)
    const yLabels = [0, 0.5, 1].map(f => ({
      y:     yOf(maxY * f).toFixed(1),
      label: Math.round(maxY * f) + '€',
    }));

    // puntos de tooltip
    const puntos = dias.map((d, i) => ({
      x: xOf(i),
      yI: yOf(d.ingresos),
      yG: yOf(d.gastos),
      label: d.label,
      servicios: d.servicios,
      ingresos: d.ingresos,
      gastos: d.gastos,
    }));

    return { pathI: toPath(ptsI), pathG: toPath(ptsG), areaI, areaG, yLabels, puntos, W, H, xOf, baseY };
  });

  // ── COSTOS TOTALES ────────────────────────────────────────────────────────────
  totalIngresos = computed(() => this.lineChartDias().reduce((s, x) => s + ((x as any).ingresos ?? 0), 0));
  totalGastos   = computed(() => this.lineChartDias().reduce((s, x) => s + (x.gastos ?? 0), 0));
  totalBeneficios = computed(() => this.totalIngresos() - this.totalGastos());

  // ── TABLA & RANKING ───────────────────────────────────────────────────────────
  ultimosServicios = computed(() =>
    [...this.servicios()].sort((a, b) => b.id - a.id).slice(0, 5)
  );

  topTrabajadores = computed(() =>
    [...this.usuarios()]
      .filter(u => u.rol == "T")
      .sort((a, b) => (b.serv_completados_total ?? 0) - (a.serv_completados_total ?? 0))
      .slice(0, 6)
  );

  maxServCompletados = computed(() =>
    Math.max(...this.topTrabajadores().map(u => u.serv_completados_total ?? 0), 1)
  );

  // ── HELPERS ───────────────────────────────────────────────────────────────────
  redirigir(ruta: string) { this.router.navigate([`/admin/${ruta}`]); }

  private recargaIntervalo: ReturnType<typeof setInterval> | null = null;


  async ngOnInit() {
    this.cargarTodo()
    this.recargaIntervalo = setInterval(() => this.cargarSegundoPlano(), 10000);
  }

  ngOnDestroy(){
    if(this.recargaIntervalo)
      clearInterval(this.recargaIntervalo)
  }

  async cargarTodo() {
    this.finishedLoading.set(false);
    await this.cargarUsuarios();
    await this.cargarServicios();
    await this.cargarVehiculos();
    this.finishedLoading.set(true);
  }

  async cargarSegundoPlano() {
    await this.cargarUsuarios();
    await this.cargarServicios();
    await this.cargarVehiculos();
  }

  async cargarServicios() {
    this.errorMsg.set('');
    try {
      const data = await this.supabaseService.getAll(Tablas.SERVICIOS);
      this.servicios.set(data.map((u: any) => new Servicio(u)));
    } catch (err: any) { this.errorMsg.set('Error al cargar servicios: ' + err.message); }
  }

  async cargarVehiculos() {
    try {
      const data = await this.supabaseService.getAll(Tablas.VEHICULOS);
      this.vehiculos.set(data.map((v: any) => new Vehiculo(v)));
    } catch (err: any) { this.errorMsg.set('Error al cargar vehículos: ' + err.message); }
  }

  async cargarUsuarios() {
    try {
      const data = await this.supabaseService.getAll(Tablas.USUARIOS);
      this.usuarios.set(data.map((v: any) => new Usuario(v)));
    } catch (err: any) { this.errorMsg.set('Error al cargar usuarios: ' + err.message); }
  }
}