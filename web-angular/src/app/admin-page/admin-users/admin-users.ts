import { Component, OnInit, OnDestroy, signal, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupabaseService } from '../../services/supabase.service';
import { DataService } from '../../services/data.service';
import { Tablas, Usuario } from '../../services/tablas.supabase';

@Component({
  selector: 'app-admin-users',
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsers implements OnInit, OnDestroy {

  finishedLoading = signal(false);
  usersFiltered   = signal<Usuario[]>([]);
  errorMsg        = signal('');

  searchQuery = '';
  showConfirmation = false;
  selected: any = null;
  refreshInterval: any;

  opcionesRol = [
    { value: 'A', label: 'Admin' },
    { value: 'U', label: 'Usuario' },
    { value: 'T', label: 'Trabajador' },
  ];
  filtrosRol = new Set<string>();
  dropdownRolAbierto = false;

  opcionesDisp = [
    { value: 'Disponible',   label: 'Disponible' },
    { value: 'En servicio',  label: 'En servicio' },
    { value: 'Inactivo',     label: 'Inactivo' },
  ];
  filtrosDisp = new Set<string>();
  dropdownDispAbierto = false;

  constructor(
    private supabaseService: SupabaseService,
    private dataService: DataService
  ) {}

  async ngOnInit() {
    await this.loadUsers();
  }

  ngOnDestroy() {
    clearInterval(this.refreshInterval);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent) {
    const target = e.target as HTMLElement;
    if (!target.closest('.filtro-dropdown')) {
      this.dropdownRolAbierto  = false;
      this.dropdownDispAbierto = false;
    }
  }

  async loadUsers() {
    this.finishedLoading.set(false);
    this.errorMsg.set('');
    try {
      await this.dataService.cargarUsuarios();
      this.filtrar();
    } catch (e: any) {
      this.errorMsg.set(e.message);
    } finally {
      this.finishedLoading.set(true);
    }
  }

  // Accede al signal del DataService
  get users(): Usuario[] {
    return this.dataService.usuarios();
  }

  filtrar() {
    const t = this.searchQuery.trim().toLowerCase();
    this.usersFiltered.set(
      this.users.filter(u => {
        const matchTexto =
          !t ||
          (u.id?.toString() ?? '').includes(t) ||
          (u.nombreCompleto ?? '').toLowerCase().includes(t);
        const matchRol  = this.filtrosRol.size === 0  || this.filtrosRol.has(u.rol);
        const matchDisp = this.filtrosDisp.size === 0 || this.filtrosDisp.has(u.disponibilidad);
        return matchTexto && matchRol && matchDisp;
      })
    );
  }

  // ── El resto sin cambios ──

  hayFiltrosActivos(): boolean {
    return this.filtrosRol.size > 0 || this.filtrosDisp.size > 0 || this.searchQuery.trim() !== '';
  }

  toggleDropdownRol() { this.dropdownRolAbierto = !this.dropdownRolAbierto; this.dropdownDispAbierto = false; }
  toggleFiltroRol(value: string) {
    this.filtrosRol.has(value) ? this.filtrosRol.delete(value) : this.filtrosRol.add(value);
    this.filtrosRol = new Set(this.filtrosRol);
    this.filtrar();
  }
  limpiarRol() { this.filtrosRol = new Set(); this.filtrar(); }

  toggleDropdownDisp() { this.dropdownDispAbierto = !this.dropdownDispAbierto; this.dropdownRolAbierto = false; }
  toggleFiltroDisp(value: string) {
    this.filtrosDisp.has(value) ? this.filtrosDisp.delete(value) : this.filtrosDisp.add(value);
    this.filtrosDisp = new Set(this.filtrosDisp);
    this.filtrar();
  }
  limpiarDisp() { this.filtrosDisp = new Set(); this.filtrar(); }

  limpiarTodo() { this.filtrosRol = new Set(); this.filtrosDisp = new Set(); this.searchQuery = ''; this.filtrar(); }

  abrirEliminar(u: any) { this.selected = u; this.showConfirmation = true; }
  cerrarEliminar() { this.selected = null; this.showConfirmation = false; }

  async confirmarEliminar() {
    if (!this.selected) return;
    try {
      await this.supabaseService.deleteRow(Tablas.USUARIOS, 'id', this.selected.id);
      this.dataService.invalidarUsuarios();
      this.cerrarEliminar();
      await this.loadUsers();
    } catch (e: any) {
      this.errorMsg.set(e.message ?? 'No se pudo eliminar el usuario.');
    }
  }
}