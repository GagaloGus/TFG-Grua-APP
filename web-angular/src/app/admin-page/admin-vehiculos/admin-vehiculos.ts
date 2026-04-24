import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Tablas, Usuario, Vehiculo } from '@services/tablas.supabase';
import { SupabaseService } from '@services/supabase.service';

@Component({
  selector: 'app-admin-vehiculos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin-vehiculos.html',
  styleUrl: './admin-vehiculos.scss',
})
export class AdminVehiculos implements OnInit {

  // ── Signals
  finishedLoading = signal(false);
  vehiculos       = signal<Vehiculo[]>([]);
  usuarios        = signal<Usuario[]>([]);
  errorMsg        = signal('');
  successMsg      = signal('');
  modalErrorMsg   = signal('');

  // ── Local
  showConfirmacion = false;
  showFormModal    = false;
  showAsignarTrabajador = false;
  modoEdicion   = false;
  selected: Vehiculo | null = null;
  formData = Vehiculo.empty();

  constructor(private supabaseService: SupabaseService) {}

  private recargaIntervalo: ReturnType<typeof setInterval> | null = null;


  async ngOnInit() {
    this.cargarTodo()
    this.recargaIntervalo = setInterval(() => this.cargarSegundoPlano(), 10000);
  }

  ngOnDestroy(){
    if(this.recargaIntervalo)
      clearInterval(this.recargaIntervalo)
  }

  async cargarTodo(){
    this.finishedLoading.set(false);
    await this.cargarVehiculos()
    await this.cargarUsuarios()
    this.finishedLoading.set(true);
  }

  async cargarSegundoPlano(){
    await this.cargarUsuarios()
    await this.cargarVehiculos()
  }

  // ── Carga
  async cargarVehiculos() {
    this.errorMsg.set('');
    try {
      const data = await this.supabaseService.getAll(Tablas.VEHICULOS);
      this.vehiculos.set(data.map((v: any) => new Vehiculo(v)));
    } catch (err: any) {
      this.errorMsg.set('Error al cargar vehiculos:' + err.message);
      console.log('Error al cargar vehiculos:' + err.message);
    }
  }

  async cargarUsuarios() {
    try {
      const data = await this.supabaseService.getAll(Tablas.USUARIOS);
      this.usuarios.set(data.map((u: any) => new Usuario(u)));
    } catch (err: any) {
      this.errorMsg.set('Error al cargar usuarios:' + err.message);
      console.error('Error al cargar usuarios:', err.message);
    }
  }

  getUsuariosAsignados(v: Vehiculo): Usuario[] {
    let u = this.usuarios().filter(u => u.vehiculo_asignado === v.matricula);
    return u ?? []
  }

    // ── Ordenar
  sortCol = signal<string | null>(null);
  sortAsc = signal(true);

  toggleSort(col: string) {
    if (this.sortCol() === col) {
      this.sortAsc.update(v => !v);
    } else {
      this.sortCol.set(col);
      this.sortAsc.set(true);
    }
  }

  // ── Filtrado
  searchQuery   = signal('');
  filtroDisp    = signal('');

  vehiculosFiltrados = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const disp = this.filtroDisp();

    //filtrado
    let result = this.vehiculos().filter(v => {
      const matchQuery =
        !q ||
        String(v.id).includes(q) ||
        (v.matricula ?? '').toLowerCase().includes(q) ||
        (v.marca ?? '').toLowerCase().includes(q) ||
        (v.zona_trabajo ?? '').toLowerCase().includes(q);
      const matchDisp =
        !disp ||
        (disp === 'disponible' && v.disponible) ||
        (disp === 'no-disponible' && !v.disponible) ||
        (disp === 'activo' && v.activo) ||
        (disp === 'inactivo' && !v.activo);
      return matchQuery && matchDisp;
    })

    const col = this.sortCol(); //ordenar
    if (col) {
      const asc = this.sortAsc() ? 1 : -1;
      result = [...result].sort((a, b) => {
        const va = (a as any)[col] ?? '';
        const vb = (b as any)[col] ?? '';
        if (va < vb) return -1 * asc;
        if (va > vb) return 1 * asc;
        return 0;
      });
    }

    return result;
  });

  btnFiltrarEstado(estado: string) {
    this.filtroDisp.set(estado == this.filtroDisp() ? '' : estado)
  }

  contarDisponibles(): number {
    return this.vehiculos().filter(v => v.disponible).length;
  }

  contarActivos(): number {
    return this.vehiculos().filter(v => v.activo).length;
  }

  // ── Modal base
  abrirModal(v: Vehiculo) {
    this.selected = new Vehiculo({ ...v });
    this.errorMsg.set('');
    this.successMsg.set('');
    this.modalErrorMsg.set('');
  }

  cerrarModal() {
    this.selected = null;
  }

  // ── Eliminar
  abrirEliminar(v: Vehiculo) {
    this.abrirModal(v);
    this.showConfirmacion = true;
  }

  cerrarEliminar() {
    this.cerrarModal();
    this.showConfirmacion = false;
  }

  async confirmarEliminar() {
    if (!this.selected) { this.cerrarEliminar(); return; }
    try {
      await this.supabaseService.deleteRow(Tablas.VEHICULOS, 'id', this.selected.id);
      this.cerrarEliminar();
      await this.cargarVehiculos();
      this.successMsg.set('Vehículo eliminado correctamente');
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al eliminar');
    }
  }

  // ── Asignar trabajador
  async abrirAsignar(v: Vehiculo) {
    this.abrirModal(v);
    this.formData = new Vehiculo({ ...v });
    this.showAsignarTrabajador = true;
    await this.cargarUsuarios();
  }

  cerrarAsignar() {
    this.cerrarModal();
    this.showAsignarTrabajador = false;
  }

  async confirmarAsignar() {
    /*if (this.formData.num_empleado == null) {
      this.modalErrorMsg.set('Para guardar debes asignar algún trabajador');
      return;
    }
    try {
      await this.supabaseService.update(
        Tablas.VEHICULOS, 'id', this.formData.id.toString(),
        { num_empleado: this.formData.num_empleado }
      );
      await this.cargarVehiculos();
      this.successMsg.set('Trabajador asignado correctamente');
    } catch (err: any) {
      this.modalErrorMsg.set(`Error al asignar: ${err.message}`);
    }*/
    this.cerrarAsignar();
  }

  // ── Crear / Editar
  abrirCrear() {
    this.abrirModal(Vehiculo.empty());
    this.modoEdicion = false;
    this.formData = Vehiculo.empty();
    this.showFormModal = true;
  }

  abrirEditar(v: Vehiculo) {
    this.abrirModal(Vehiculo.empty());
    this.modoEdicion = true;
    this.formData = new Vehiculo({ ...v });
    this.showFormModal = true;
  }

  cerrarForm() {
    this.cerrarModal();
    this.showFormModal = false;
  }

  async guardarVehiculo(): Promise<void> {
    const validacion = this.validarVehiculo();
    if (validacion !== '') {
      this.modalErrorMsg.set(validacion);
      return;
    }
    try {
      if (this.modoEdicion) {
        await this.supabaseService.update(Tablas.VEHICULOS, 'id', this.formData.id.toString(), this.formData);
        this.successMsg.set('Vehículo editado correctamente');
      } else {
        await this.supabaseService.insert(Tablas.VEHICULOS, this.formData);
        this.successMsg.set('Vehículo creado correctamente');
      }
      this.cerrarForm();
      await this.cargarVehiculos();
    } catch (err: any) {
      this.modalErrorMsg.set(`Error al guardar: ${err.message}`);
    }
  }

  validarVehiculo(): string {
    if (!this.formData.matricula) return 'La matrícula es obligatoria';
    return '';
  }
}