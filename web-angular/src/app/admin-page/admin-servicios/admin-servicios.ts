import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Roles, Servicio, Tablas, Usuario, Vehiculo } from '../../services/tablas.supabase';
import { SupabaseService } from '../../services/supabase.service';
import { MapPickerComponent } from '../../shared/map-picker/map-picker';
import { PRECIO_LITRO_COMBUSTIBLE } from '@services/global/global.service';

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe, MapPickerComponent],
  templateUrl: './admin-servicios.html',
  styleUrl: './admin-servicios.scss',
})
export class AdminServicios implements OnInit {
  constructor(private supabaseService: SupabaseService) { }

  // ── Signals — se actualizan solos sin Zone.js
  finishedLoading = signal(false);
  servicios = signal<Servicio[]>([]);
  vehiculos = signal<Vehiculo[]>([]);
  usuarios = signal<Usuario[]>([]);
  successMsg = signal('');
  errorMsg = signal('');
  modalErrorMsg = signal('')

  // ── Local
  showDetalles = false;
  showConfirmacion = false;
  showFormModal = false;
  modoEdicion = false;
  selected: Servicio | null = null;
  usuarios_vehiculo: Usuario[] | null = [];
  formData = Servicio.empty();

  private recargaIntervalo: ReturnType<typeof setInterval> | null = null;

  async ngOnInit() {
    this.cargarTodo()
    this.recargaIntervalo = setInterval(() => this.cargarSegundoPlano(), 10000);
  }

  ngOnDestroy() {
    if (this.recargaIntervalo)
      clearInterval(this.recargaIntervalo)
  }

  async cargarTodo() {
    this.finishedLoading.set(false);
    await this.cargarUsuarios()
    await this.cargarServicios()
    await this.cargarVehiculos()
    this.finishedLoading.set(true);
  }

  async cargarSegundoPlano() {
    await this.cargarUsuarios()
    await this.cargarServicios()
    await this.cargarVehiculos()
  }

  async cargarServicios() {
    try {
      const data = await this.supabaseService.getAll(Tablas.SERVICIOS);
      this.servicios.set(data.map((u: any) => new Servicio(u)));
    } catch (err: any) {
      this.errorMsg.set('Error al cargar servicios');
      console.error('Error al cargar servicios:', err.message);
    }
  }

  async cargarVehiculos() {
    try {
      const data = await this.supabaseService.getAll(Tablas.VEHICULOS);
      this.vehiculos.set(data.map((v: any) => new Vehiculo(v)));
    } catch (err: any) {
      this.errorMsg.set('Error al cargar vehiculos');
      console.error('Error al cargar vehiculos:', err.message);
    }
  }

  async cargarUsuarios() {
    try {
      const data = await this.supabaseService.getAll(Tablas.USUARIOS);
      this.usuarios.set(data.map((v: any) => new Usuario(v)));
    } catch (err: any) {
      this.errorMsg.set('Error al cargar usuarios');
      console.error('Error al cargar usuarios:', err.message);
    }
  }

  getUserByNumEmpleado(numEmpleado: number): Usuario | undefined {
    return this.usuarios().find(u => u.num_empleado === numEmpleado);
  }
  getVehiculo(matricula: string): Vehiculo | undefined {
    return this.vehiculos().find(v => v.matricula === matricula);
  }

  onVehiculoChange() {
    const u = this.usuarios().filter(u =>
      u.rol == 'T' && u.vehiculo_asignado == this.formData.vehiculo_matricula
    );
    this.usuarios_vehiculo = u.length > 0 ? u : null;
    this.calcularCostoAproxViaje();
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
  searchQuery = signal('');
  filtroEstado = signal('');

  serviciosFiltrados = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();

    //filtrado
    let result = this.servicios().filter(s => {
      const matchQuery =
        !q ||
        String(s.id).includes(q) ||
        (s.nombre_cliente ?? '').toLowerCase().includes(q) ||
        (s.observaciones ?? '').toLowerCase().includes(q) ||
        (s.tel_cliente ?? '').includes(q);
      const matchEstado = !this.filtroEstado() || s.estado === this.filtroEstado();
      return matchQuery && matchEstado;
    });

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
    this.filtroEstado.set(estado == this.filtroEstado() ? '' : estado)
  }

  contarEstado(estado: string): number {
    return this.servicios().filter(s => s.estado === estado).length;
  }

  // ── Modals

  abrirModal(s: Servicio) {
    this.selected = new Servicio({ ...s });
    this.errorMsg.set('')
    this.successMsg.set('')
    this.modalErrorMsg.set('')
  }

  cerrarModal() {
    this.selected = null;
    this.formData = Servicio.empty();
  }


  // ── Detalles
  abrirDetalles(s: Servicio) {
    this.abrirModal(s);
    this.showDetalles = true;
  }

  cerrarDetalles() {
    this.cerrarModal();
    this.showDetalles = false;
  }
  // ── Duplicar
  async duplicarServicio(s: Servicio) {
    try {
      await this.supabaseService.insert(Tablas.SERVICIOS, new Servicio({ ...s }));
      this.cerrarForm();
      this.successMsg.set(`Servicio duplicado correctamente`)
      await this.cargarServicios();
    } catch (err: any) {
      this.errorMsg.set(`Error al duplicar: ${err.message}`);
    }
  }

  // ── Eliminar
  abrirEliminar(s: Servicio) {
    this.abrirModal(s)
    this.showConfirmacion = true;
  }

  cerrarEliminar() {
    this.cerrarModal()
    this.showConfirmacion = false;
  }

  async confirmarEliminar() {
    if (!this.selected) {
      this.cerrarEliminar()
      return
    }

    try {
      await this.supabaseService.deleteRow(Tablas.SERVICIOS, 'id', this.selected.id);
      this.cerrarEliminar();
      this.successMsg.set(`Servicio eliminado correctamente`)
      await this.cargarUsuarios()
      window.location.reload()
    } catch (err: any) {
      this.errorMsg.set('Error al eliminar: ' + err.message);
    }
  }

  // ── Crear / Editar
  async abrirCrear() {
    this.modoEdicion = false;
    this.formData = Servicio.empty();
    this.abrirModal(this.formData)
    this.showFormModal = true;
    await this.cargarVehiculos()
    this.onVehiculoChange()
  }

  async abrirEditar(s: Servicio) {
    this.modoEdicion = true;
    this.formData = new Servicio({ ...s });
    this.abrirModal(this.formData)
    this.showFormModal = true;
    await this.cargarVehiculos()
    this.onVehiculoChange()
  }

  cerrarForm() {
    this.cerrarModal()
    this.showFormModal = false;
  }

  async guardarServicio(): Promise<void> {
    const validacion = this.validarServicio();
    if (validacion != "") {
      this.modalErrorMsg.set(validacion);
      return;
    }

    try {
      //Editando un servicio
      if (this.modoEdicion) {
        await this.supabaseService.update(Tablas.SERVICIOS, 'id', this.formData.id.toString(), this.formData);
        this.successMsg.set(`Servicio editado correctamente`)
      }
      //Añadiendo un servicio
      else {
        await this.supabaseService.insert(Tablas.SERVICIOS, this.formData);
        this.successMsg.set(`Servicio creado correctamente`)
      }

      this.cerrarForm();
      await this.cargarServicios();
    } catch (err: any) {
      this.modalErrorMsg.set(`Error al guardar: ${err.message}`);
    }
  }

  async calcularCostoAproxViaje() {
    // No se le ha asignado un vehiculo al servicio
    if (!this.formData.vehiculo_matricula) {
      this.formData.costo = 0
      return
    }

    // El coste ya fue establecido manualmente
    if(this.formData.costo != 0){
      return
    }

    let v = this.getVehiculo(this.formData.vehiculo_matricula)
    if(!v){
      this.formData.costo = 0
      return
    }

    console.log(this.formData.distancia_real)
    this.formData.costo = Math.round(this.formData.distancia_real * v.litro_combustible_km * PRECIO_LITRO_COMBUSTIBLE * 100)/100
  }

  validarServicio(): string {
    if (!this.formData.nombre_cliente) return "El nombre del cliente es obligatorio";
    if (!this.formData.tel_cliente) return "El teléfono del cliente es obligatorio";
    if (!this.formData.ubicacion_recogida_lat || !this.formData.ubicacion_recogida_lng) return "La ubicación de recogida es obligatoria";
    if (!this.formData.ubicacion_destino_lat || !this.formData.ubicacion_destino_lng) return "La ubicación de destino es obligatoria";

    return '';
  }

    // ── Mapa
  showMapPicker = false;
  mapPickerTarget: 'recogida' | 'destino' | null = null;
  mapPickerLat = 0;
  mapPickerLng = 0;

  abrirMapa(target: 'recogida' | 'destino') {
  this.mapPickerTarget = target;
  if (target === 'recogida') {
    this.mapPickerLat = this.formData.ubicacion_recogida_lat ?? 40.4168;
    this.mapPickerLng = this.formData.ubicacion_recogida_lng ?? -3.7038;
  } else {
    this.mapPickerLat = this.formData.ubicacion_destino_lat ?? 40.4168;
    this.mapPickerLng = this.formData.ubicacion_destino_lng ?? -3.7038;
  }
  this.showMapPicker = true;
}

onUbicacionSeleccionada(data: { lat: number; lng: number; direccion: string }) {
  if (this.mapPickerTarget === 'recogida') {
    this.formData.ubicacion_recogida_lat = data.lat;
    this.formData.ubicacion_recogida_lng = data.lng;
    if (!this.formData.direccion_ubi_recogida) {
      this.formData.direccion_ubi_recogida = data.direccion;
    }
  } else if (this.mapPickerTarget === 'destino') {
    this.formData.ubicacion_destino_lat = data.lat;
    this.formData.ubicacion_destino_lng = data.lng;
    if (!this.formData.direccion_ubi_destino) {
      this.formData.direccion_ubi_destino = data.direccion;
    }
  }
  this.showMapPicker = false;
  this.mapPickerTarget = null;
}
}