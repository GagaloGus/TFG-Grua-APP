import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Servicio, Tablas, Usuario, Vehiculo } from '../../services/tablas.supabase';
import { SupabaseService } from '../../services/supabase.service';

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe],
  templateUrl: './admin-servicios.html',
  styleUrl: './admin-servicios.scss',
})
export class AdminServicios implements OnInit {

  // ── Signals — se actualizan solos sin Zone.js
  finishedLoading = signal(false);
  servicios = signal<Servicio[]>([]);
  serviciosFiltrados = signal<Servicio[]>([]);
  vehiculos = signal<Vehiculo[]>([]);
  usuarios = signal<Usuario[]>([]);
  successMsg = signal('');
  errorMsg = signal('');
  modalErrorMsg = signal('')

  // ── Local
  searchQuery = '';
  filtroEstado = '';
  showConfirmacion = false;
  showFormModal = false;
  showAsignarTrabajador = false;
  modoEdicion = false;
  selected: Servicio | null = null;
  formData = Servicio.empty();
  constructor(private supabaseService: SupabaseService) { }

  ngOnInit() {
    this.cargarTodo()
  }


  async cargarTodo(){
    this.finishedLoading.set(false);
    await this.cargarUsuarios()
    await this.cargarServicios()
    this.finishedLoading.set(true);
  }


  async cargarServicios() {
    this.errorMsg.set('');
    try {
      const data = await this.supabaseService.getAll(Tablas.SERVICIOS);
      this.servicios.set(data.map((u: any) => new Servicio(u)));
      this.filtrar();
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al cargar servicios');
    }
  }

  async cargarVehiculos() {
    try {
      const data = await this.supabaseService.getAll(Tablas.VEHICULOS);
      this.vehiculos.set(data.map((v: any) => new Vehiculo(v)));
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al cargar vehiculos');
      console.error('Error al cargar vehiculos:', err.message);
    }
  }

  async cargarUsuarios() {
    try {
      const data = await this.supabaseService.getAll(Tablas.USUARIOS);
      this.usuarios.set(data.map((v: any) => new Usuario(v)));
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al cargar usuarios');
      console.error('Error al cargar usuarios:', err.message);
    }
  }

  getUserByNumEmpleado(numEmpleado: number): Usuario | undefined {
    return this.usuarios().find(u => u.num_empleado === numEmpleado);
  }

  // ── Filtrado
  filtrar() {
    const q = this.searchQuery.toLowerCase().trim();
    this.serviciosFiltrados.set(
      this.servicios().filter(s => {
        const matchQuery =
          !q ||
          String(s.id).includes(q) ||
          (s.nombre_cliente ?? '').toLowerCase().includes(q) ||
          (s.observaciones ?? '').toLowerCase().includes(q) ||
          (s.tel_cliente ?? '').includes(q);
        const matchEstado = !this.filtroEstado || s.estado === this.filtroEstado;
        return matchQuery && matchEstado;
      })
    );
  }


  contarEstado(estado: string): number {
    return this.servicios().filter(s => s.estado === estado).length;
  }

  // ── Modals

  abrirModal(s: Servicio){
    this.selected = new Servicio({...s});
    this.errorMsg.set('')
    this.successMsg.set('')
    this.modalErrorMsg.set('')
  }

  cerrarModal(){
    this.selected = null;
  }

  // ── Duplicar
  async duplicarServicio(s: Servicio){
    try {
      await this.supabaseService.insert(Tablas.SERVICIOS, new Servicio({...s}));
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
    if (!this.selected){
      this.cerrarEliminar()
      return
    }

    try {
      await this.supabaseService.deleteRow(Tablas.SERVICIOS, 'id', this.selected.id);
      this.cerrarEliminar();
      await this.cargarUsuarios()
      this.successMsg.set(`Servicio eliminado correctamente`)
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al eliminar');
    }
  }


  // ── Asignar trabajador
  async abrirAsignar(s: Servicio) {
    this.abrirModal(s)
    this.showAsignarTrabajador = true;
    await this.cargarUsuarios()
  }

  cerrarAsignar(){
    this.cerrarModal()
    this.showAsignarTrabajador = false;
  }

  async confirmarAsignar(){
    if(!this.formData.num_empleado){
      this.modalErrorMsg.set('Para guardar debes asignar algun trabajador!')
      return
    }

    try {
      await this.supabaseService.update(Tablas.SERVICIOS, 'id', this.formData.id.toString(), {num_empleado: this.formData.num_empleado});
      this.cerrarAsignar();
      await this.cargarServicios();
      this.successMsg.set(`Trabajador asignado correctamente`)
    } catch (err: any) {
      this.modalErrorMsg.set(`Error al asignar: ${err.message}`);
    }

  }

  // ── Crear / Editar
  async abrirCrear() {
    this.abrirModal(new Servicio({}))
    this.modoEdicion = false;
    this.formData = Servicio.empty();
    this.showFormModal = true;
    await this.cargarVehiculos()
  }

  async abrirEditar(s: Servicio) {
    this.abrirModal(new Servicio({}))
    this.modoEdicion = true;
    this.formData = new Servicio({...s});
    this.showFormModal = true;
    await this.cargarVehiculos()
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

  validarServicio(): string {
    if (!this.formData.nombre_cliente) return "El nombre del cliente es obligatorio";
    if (!this.formData.tel_cliente) return "El teléfono del cliente es obligatorio";
    if (!this.formData.ubicacion_recogida_lat || !this.formData.ubicacion_recogida_lng) return "La ubicación de recogida es obligatoria";
    if (!this.formData.ubicacion_destino_lat || !this.formData.ubicacion_destino_lng) return "La ubicación de destino es obligatoria";
    if (!this.formData.vehiculo_matricula) return "La matrícula del vehículo es obligatoria";

    return '';
  }
}