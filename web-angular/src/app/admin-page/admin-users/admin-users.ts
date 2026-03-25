import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupabaseService } from '../../services/supabase.service';

@Component({
  selector: 'app-admin-users',
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css',
})
export class AdminUsers implements OnInit {

  users: any[] = [];         // lista completa
  usersFiltered: any[] = []; // lista que se muestra
  searchQuery: string = '';
  errorMsg: string = '';

  showConfirmation = false;
  selected: any = null;

  constructor(private supabaseService: SupabaseService) {}

  async ngOnInit() {
    await this.loadUsers();
  }

  async loadUsers() {
    try {
      this.users = await this.supabaseService.getAllUsers();
      this.filtrar(); // sincroniza la lista visible
    } catch (e) {
      this.errorMsg = 'No se pudieron cargar los usuarios.';
      console.error(e);
    }
  }

  filtrar() {
    const t = this.searchQuery.trim().toLowerCase();
    if (!t) {
      this.usersFiltered = this.users;
      return;
    }
    this.usersFiltered = this.users.filter(u =>
      (u.id?.toString() ?? '').includes(t)
    );
  }

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
      await this.supabaseService.deleteUser(this.selected.id);
      this.cerrarEliminar();
      await this.loadUsers();
    } catch (e) {
      this.errorMsg = 'No se pudo eliminar el usuario.';
      console.error(e);
    }
  }
}