import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SupabaseService } from '@services/supabase.service';
import { Servicio, Vehiculo, Usuario, Tablas } from '@services/tablas.supabase';

@Component({
  selector: 'app-admin-dashboard',
  imports: [FormsModule],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
})
export class AdminDashboard {
    // ── Signals — se actualizan solos sin Zone.js
  finishedLoading = signal(false);
  servicios = signal<Servicio[]>([]);
  vehiculos = signal<Vehiculo[]>([]);
  usuarios = signal<Usuario[]>([]);
  successMsg = signal('');
  errorMsg = signal('');
  modalErrorMsg = signal('')

  constructor(private supabaseService: SupabaseService, private router: Router) { }

  contarTrabajDisponibles(): number {
    return this.usuarios().filter(u => u.disponibilidad == "Disponible").length;
  }
  contarServiciosActivos(): number {
    return this.servicios().filter(s => s.estado == "En curso").length;
  }
  contarServiciosCompletados(): number {
    return this.servicios().filter(s => s.estado == "Terminado").length;
  }
  contarVehiculosDisponibles(): number {
    return this.vehiculos().filter(v => v.disponible).length;
  }

  redirigir(ruta: string){
    this.router.navigate([`/home/admin/${ruta}`])
  }

  ngOnInit() {
    this.cargarTodo()
  }

  async cargarTodo() {
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
    } catch (err: any) {
      this.errorMsg.set('Error al cargar servicios: ' + err.message);
      console.error('Error al cargar servicios: ' + err.message);
    }
  }

  async cargarVehiculos() {
    try {
      const data = await this.supabaseService.getAll(Tablas.VEHICULOS);
      this.vehiculos.set(data.map((v: any) => new Vehiculo(v)));
    } catch (err: any) {
      this.errorMsg.set('Error al cargar vehiculos: ' + err.message);
      console.error('Error al cargar vehiculos:', err.message);
    }
  }

  async cargarUsuarios() {
    try {
      const data = await this.supabaseService.getAll(Tablas.USUARIOS);
      this.usuarios.set(data.map((v: any) => new Usuario(v)));
    } catch (err: any) {
      this.errorMsg.set('Error al cargar usuarios: ' + err.message);
      console.error('Error al cargar usuarios:', err.message);
    }
  }
}
