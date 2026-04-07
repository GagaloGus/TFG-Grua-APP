import { Component, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Servicio, Tablas } from '../../services/tablas.supabase';
import { SupabaseService } from '../../services/supabase.service';

interface FormData {
  id?: number;
  nombre_cliente: string;
  tel_cliente: string;
  ubicacion_recogida_str: string;
  ubicacion_destino_str: string;
  estado: string;
  vehiculo_id: number | null;
  observaciones: string;
}

@Component({
  selector: 'app-servicios',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, DatePipe],
  templateUrl: './admin-servicios.html',
  styleUrl: './admin-servicios.scss',
})
export class AdminServicios implements OnInit {

  // ── Signals — se actualizan solos sin Zone.js
  finishedLoading    = signal(false);
  servicios          = signal<Servicio[]>([]);
  serviciosFiltrados = signal<Servicio[]>([]);
  errorMsg           = signal('');

  // ── Local
  searchQuery      = '';
  filtroEstado     = '';
  showConfirmacion = false;
  selected: Servicio | null = null;
  showFormModal    = false;
  modoEdicion      = false;
  formData: FormData = this.emptyForm();

  constructor(private supabaseService: SupabaseService) {}

  ngOnInit() {
    this.cargarServicios();
  }

  async cargarServicios(){
    this.finishedLoading.set(false);
    this.errorMsg.set('');
    try {
      const data = await this.supabaseService.getAll(Tablas.SERVICIOS);
      this.servicios.set(data.map((u: any) => new Servicio(u)));
      this.filtrar();
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al cargar servicios');
    } finally {
      this.finishedLoading.set(true);
    }
  }

  // ── Filtrado
  filtrar(){
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

  // ── Coordenadas
  formatCoords(coords: number[]): string {
    if (!coords || coords.length < 2) return '—';
    return `${coords[0].toFixed(4)}, ${coords[1].toFixed(4)}`;
  }

  getBadgeClass(estado: string): string {
    const map: Record<string, string> = {
      'Sin empezar': 'estado-sin-empezar',
      'En curso':    'estado-en-curso',
      'Finalizado':  'estado-finalizado',
      'Cancelado':   'estado-cancelado',
    };
    return map[estado] ?? '';
  }

  // ── Eliminar
  abrirEliminar(s: Servicio){
    this.selected = s;
    this.showConfirmacion = true;
  }

  cerrarEliminar(){
    this.showConfirmacion = false;
    this.selected = null;
  }

  async confirmarEliminar(){
    if (!this.selected) return;
    try {
      await this.supabaseService.deleteRow(Tablas.SERVICIOS, 'id', this.selected.id);
      this.servicios.set(this.servicios().filter(s => s.id !== this.selected!.id));
      this.filtrar();
      this.cerrarEliminar();
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al eliminar');
    }
  }

  // ── Crear / Editar
  abrirCrear(){
    this.modoEdicion = false;
    this.formData = this.emptyForm();
    this.showFormModal = true;
  }

  abrirEditar(s: Servicio){
    this.modoEdicion = true;
    this.formData = {
      id:                     s.id,
      nombre_cliente:         s.nombre_cliente ?? '',
      tel_cliente:            s.tel_cliente ?? '',
      ubicacion_recogida_str: s.ubicacion_recogida.join(', '),
      ubicacion_destino_str:  s.ubicacion_destino.join(', '),
      estado:                 s.estado,
      vehiculo_id:            s.vehiculo_id,
      observaciones:          s.observaciones ?? '',
    };
    this.showFormModal = true;
  }

  cerrarForm(){
    this.showFormModal = false;
  }

  async guardarServicio(): Promise<void> {
    const recogida = this.parseCoordenadas(this.formData.ubicacion_recogida_str);
    const destino  = this.parseCoordenadas(this.formData.ubicacion_destino_str);

    if (!recogida || !destino) {
      this.errorMsg.set('Coordenadas inválidas. Usa el formato: lat, lng');
      return;
    }

    const datos_carga = {
      nombre_cliente:     this.formData.nombre_cliente || null,
      tel_cliente:        this.formData.tel_cliente || null,
      ubicacion_recogida: recogida,
      ubicacion_destino:  destino,
      estado:             this.formData.estado,
      vehiculo_id:        this.formData.vehiculo_id || null,
      observaciones:      this.formData.observaciones || null,
    }

    try {
      if (this.modoEdicion && this.formData.id != null) {
        // await this.supabaseService.update(Tablas.SERVICIOS, 'id', this.formData.id, payload);
        this.servicios.set(
          this.servicios().map(s =>
            s.id === this.formData.id ? { ...s, ...datos_carga } as Servicio : s
          )
        );
      } else {
        const nueva = await this.supabaseService.insert(Tablas.SERVICIOS, datos_carga);
      }

      this.cerrarForm();
      this.errorMsg.set('');
      this.cargarServicios();
    } catch (err: any) {
      this.errorMsg.set(err.message ?? 'Error al guardar');
    }
  }

  private parseCoordenadas(str: string): number[] | null {
    const parts = str.split(',').map(p => parseFloat(p.trim()));
    if (parts.length !== 2 || parts.some(isNaN)) return null;
    return parts;
  }

  private emptyForm(): FormData {
    return {
      nombre_cliente:         '',
      tel_cliente:            '',
      ubicacion_recogida_str: '',
      ubicacion_destino_str:  '',
      estado:                 'Sin empezar',
      vehiculo_id:            null,
      observaciones:          '',
    };
  }
}