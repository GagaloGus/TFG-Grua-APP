import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Servicio, Tablas, Vehiculo } from '../../services/tablas.supabase';
import { SupabaseService } from '../../services/supabase.service';
import { DataService } from '@services/data.service';

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe],
  templateUrl: './admin-servicios.html',
  styleUrl: './admin-servicios.scss',
})
export class AdminServicios implements OnInit {

  // ── Signals 
  finishedLoading = signal(false);
  serviciosFiltrados = signal<Servicio[]>([]);
  vehiculos = signal<Vehiculo[]>([]);
  errorMsg = signal('');
  createErrorMsg = signal('')

  // ── Local
  searchQuery = '';
  filtroEstado = '';
  showConfirmacion = false;
  selected: Servicio | null = null;
  showFormModal = false;
  modoEdicion = false;
  formData = Servicio.empty();
  constructor(private supabaseService: SupabaseService, private dataService: DataService) { }

  get servicios(): Servicio[] {
    return this.dataService.servicios()
  }

  ngOnInit() {
    this.cargarServicios();
  }

  async cargarServicios() {
    this.finishedLoading.set(false);
    this.errorMsg.set('');
    try {
      this.dataService.cargarServicios();
      this.filtrar();
    } catch (err: any) {
      this.errorMsg.set(err.message);
    } finally {
      this.finishedLoading.set(true);
    }
  }

  async cargarVehiculos() {
    try {
      this.dataService.cargarVehiculos()
    } catch (err: any) {
      this.errorMsg.set(err.message);
    }
  }

  // ── Filtrado
  filtrar() {
    const q = this.searchQuery.toLowerCase().trim();
    this.serviciosFiltrados.set(
      this.servicios.filter(s => {
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
    return this.servicios.filter(s => s.estado === estado).length;
  }

  // ── Eliminar
  abrirEliminar(s: Servicio) {
    this.selected = s;
    this.showConfirmacion = true;
  }

  cerrarEliminar() {
    this.showConfirmacion = false;
    this.selected = null;
  }

  async confirmarEliminar() {
    if (!this.selected) return;
    try {
      await this.supabaseService.deleteRow(Tablas.SERVICIOS, 'id', this.selected.id);
      this.dataService.invalidarServicios();
      await this.cargarServicios();
      this.cerrarEliminar();
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al eliminar');
    }
  }

  // ── Crear / Editar
  async abrirCrear() {
    this.modoEdicion = false;
    this.formData = Servicio.empty();
    this.showFormModal = true;
  }

  async abrirEditar(s: Servicio) {
    this.modoEdicion = true;
    this.formData = new Servicio({...s});
    this.showFormModal = true;
  }

  cerrarForm() {
    this.showFormModal = false;
  }

  async guardarServicio(): Promise<void> {
    // ... validación ...
    try {
      if (this.modoEdicion && this.formData.id != null) {
        await this.supabaseService.update(Tablas.SERVICIOS, 'id', this.formData.id.toString(), this.formData);
      } else {
        await this.supabaseService.insert(Tablas.SERVICIOS, this.formData);
      }
      this.dataService.invalidarServicios();
      this.cerrarForm();
      this.createErrorMsg.set('');
      await this.cargarServicios();
    } catch (err: any) {
      this.createErrorMsg.set(err.message ?? 'Error al guardar');
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