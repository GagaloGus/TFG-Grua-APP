import { Component, OnInit, OnDestroy, signal, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupabaseService } from '../../services/supabase.service';
import { Tablas, Usuario } from '../../services/tablas.supabase';

@Component({
  selector: 'app-admin-users',
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsers implements OnInit, OnDestroy {

  // ── Signals
  finishedLoading = signal(false);
  users           = signal<Usuario[]>([]);
  usersFiltered   = signal<Usuario[]>([]);
  errorMsg        = signal('');

  // ── Local
  searchQuery = '';
  showConfirmation = false;
  selected: any = null;
  refreshInterval: any;

  // ── Filtros por rol
  opcionesRol = [
    { value: 'A', label: 'Admin' },
    { value: 'U', label: 'Usuario' },
    { value: 'T', label: 'Trabajador' },
  ];
  filtrosRol = new Set<string>();
  dropdownRolAbierto = false;

  // ── Filtros por disponibilidad
  opcionesDisp = [
    { value: 'Disponible',   label: 'Disponible' },
    { value: 'En servicio',  label: 'En servicio' },
    { value: 'Inactivo',     label: 'Inactivo' },
  ];
  filtrosDisp = new Set<string>();
  dropdownDispAbierto = false;

  constructor(private supabaseService: SupabaseService) {}

  async ngOnInit() {
    await this.loadUsers();
  }

  ngOnDestroy() {
    clearInterval(this.refreshInterval);
  }

  // ── Cierra dropdowns al hacer click fuera
  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent) {
    const target = e.target as HTMLElement;
    if (!target.closest('.filtro-dropdown')) {
      this.dropdownRolAbierto  = false;
      this.dropdownDispAbierto = false;
    }
  }

  // ── Carga de datos
  async loadUsers() {
    this.finishedLoading.set(false);
    this.errorMsg.set('');
    try {
      const data = await this.supabaseService.getAll(Tablas.USUARIOS);
      this.users.set(data.map(u => new Usuario(u)));
      this.filtrar();
    } catch (e) {
      this.errorMsg.set('No se pudieron cargar los usuarios.');
      console.error(e);
    } finally {
      this.finishedLoading.set(true);
    }
  }

  // ── Filtrado
  filtrar() {
    const t = this.searchQuery.trim().toLowerCase();

    this.usersFiltered.set(
      this.users().filter(u => {
        // Búsqueda por texto
        const matchTexto =
          !t ||
          (u.id?.toString() ?? '').includes(t) ||
          (u.nombreCompleto ?? '').toLowerCase().includes(t);

        // Filtro por rol (si no hay ninguno marcado, pasan todos)
        const matchRol =
          this.filtrosRol.size === 0 || this.filtrosRol.has(u.rol);

        // Filtro por disponibilidad
        const matchDisp =
          this.filtrosDisp.size === 0 || this.filtrosDisp.has(u.disponibilidad);

        return matchTexto && matchRol && matchDisp;
      })
    );
  }

  hayFiltrosActivos(): boolean {
    return this.filtrosRol.size > 0 || this.filtrosDisp.size > 0 || this.searchQuery.trim() !== '';
  }

  // ── Dropdown Rol
  toggleDropdownRol() {
    this.dropdownRolAbierto  = !this.dropdownRolAbierto;
    this.dropdownDispAbierto = false;
  }

  toggleFiltroRol(value: string) {
    if (this.filtrosRol.has(value)) {
      this.filtrosRol.delete(value);
    } else {
      this.filtrosRol.add(value);
    }
    this.filtrosRol = new Set(this.filtrosRol); // fuerza detección de cambio
    this.filtrar();
  }

  limpiarRol() {
    this.filtrosRol = new Set();
    this.filtrar();
  }

  // ── Dropdown Disponibilidad
  toggleDropdownDisp() {
    this.dropdownDispAbierto = !this.dropdownDispAbierto;
    this.dropdownRolAbierto  = false;
  }

  toggleFiltroDisp(value: string) {
    if (this.filtrosDisp.has(value)) {
      this.filtrosDisp.delete(value);
    } else {
      this.filtrosDisp.add(value);
    }
    this.filtrosDisp = new Set(this.filtrosDisp);
    this.filtrar();
  }

  limpiarDisp() {
    this.filtrosDisp = new Set();
    this.filtrar();
  }

  limpiarTodo() {
    this.filtrosRol  = new Set();
    this.filtrosDisp = new Set();
    this.searchQuery = '';
    this.filtrar();
  }

  // ── Eliminar
  abrirEliminar(u: any) {
    this.selected = u;
    this.showConfirmation = true;
  }

  cerrarEliminar() {
    this.selected = null;
    this.showConfirmation = false;
  }

  async confirmarEliminar() {
    if (!this.selected) return;
    try {
      await this.supabaseService.deleteRow(Tablas.USUARIOS, 'id', this.selected.id);
      this.cerrarEliminar();
      await this.loadUsers();
    } catch (e) {
      this.errorMsg.set('No se pudo eliminar el usuario.');
      console.error(e);
    }
  }
}