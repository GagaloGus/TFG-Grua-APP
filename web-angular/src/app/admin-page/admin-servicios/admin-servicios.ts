import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Servicio, Tablas } from '../../services/tablas.supabase';
import { SupabaseService } from '../../services/supabase.service';
// import { SupabaseService } from '../services/supabase.service'; // descomenta cuando tengas el servicio

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

  servicios: Servicio[] = [];
  serviciosFiltrados: Servicio[] = [];

  searchQuery   = '';
  filtroEstado  = '';
  errorMsg      = '';

  // Modal eliminar
  showConfirmacion = false;
  selected: Servicio | null = null;

  // Modal crear / editar
  showFormModal = false;
  modoEdicion   = false;
  formData: FormData = this.emptyForm();

  constructor(private supabaseService: SupabaseService) {}
  // ─────────────────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.cargarServicios();
  }

  async cargarServicios(){
    const data = await this.supabaseService.getAll(Tablas.SERVICIOS)
    this.servicios = data.map(u => new Servicio(u));
    this.filtrar();
  }

  filtrar(): void {
    const q = this.searchQuery.toLowerCase().trim();
    this.serviciosFiltrados = this.servicios.filter(s => {
      const matchQuery =
        !q ||
        String(s.id).includes(q) ||
        (s.nombre_cliente ?? '').toLowerCase().includes(q) ||
        (s.observaciones ?? '').toLowerCase().includes(q) ||
        (s.tel_cliente ?? '').includes(q);

      const matchEstado = !this.filtroEstado || s.estado === this.filtroEstado;

      return matchQuery && matchEstado;
    });
  }

  contarEstado(estado: string): number {
    return this.servicios.filter(s => s.estado === estado).length;
  }

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

  // ── Modal eliminar ────────────────────────────────────────────────────────
  abrirEliminar(s: Servicio): void {
    this.selected = s;
    this.showConfirmacion = true;
  }

  cerrarEliminar(): void {
    this.showConfirmacion = false;
    this.selected = null;
  }

  async confirmarEliminar(): Promise<void> {
    if (!this.selected) 
      return;

    try {
      await this.supabaseService.deleteRow(Tablas.SERVICIOS, "id", this.selected.id)

      this.servicios = this.servicios.filter(s => s.id !== this.selected!.id);
      this.filtrar();
      this.cerrarEliminar();
    } catch (err: any) {
      this.errorMsg = err.message ?? 'Error al eliminar';
    }
  }

  // ── Modal crear / editar ──────────────────────────────────────────────────
  abrirCrear(): void {
    this.modoEdicion = false;
    this.formData = this.emptyForm();
    this.showFormModal = true;
  }

  abrirEditar(s: Servicio): void {
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

  cerrarForm(): void {
    this.showFormModal = false;
  }

  async guardarServicio(): Promise<void> {
    const recogida = this.parseCoordenadas(this.formData.ubicacion_recogida_str);
    const destino  = this.parseCoordenadas(this.formData.ubicacion_destino_str);

    if (!recogida || !destino) {
      this.errorMsg = 'Coordenadas inválidas. Usa el formato: lat, lng';
      return;
    }

    const payload = {
      nombre_cliente:     this.formData.nombre_cliente || null,
      tel_cliente:        this.formData.tel_cliente || null,
      ubicacion_recogida: recogida,
      ubicacion_destino:  destino,
      estado:             this.formData.estado,
      vehiculo_id:        this.formData.vehiculo_id || null,
      observaciones:      this.formData.observaciones || null,
    };

    try {
      if (this.modoEdicion && this.formData.id != null) {
        // const { error } = await this.supabase.client
        //   .from('servicios').update(payload).eq('id', this.formData.id);
        // if (error) throw error;

        const idx = this.servicios.findIndex(s => s.id === this.formData.id);
        
        if (idx !== -1) this.servicios[idx] = { ...this.servicios[idx], ...payload };
      } else {
        // const { data, error } = await this.supabase.client
        //   .from('servicios').insert(payload).select().single();
        // if (error) throw error;
        // this.servicios.unshift(data);

        const nuevoId = Math.max(...this.servicios.map(s => s.id)) + 1;
        this.servicios.unshift({
          id:    nuevoId,
          fecha: new Date().toISOString(),
          operador_id: null,
          ...payload,
        } as Servicio);
      }

      this.filtrar();
      this.cerrarForm();
      this.errorMsg = '';
    } catch (err: any) {
      this.errorMsg = err.message ?? 'Error al guardar';
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