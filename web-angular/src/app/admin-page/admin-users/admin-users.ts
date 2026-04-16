import { Component, OnInit, OnDestroy, signal, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupabaseService } from '../../services/supabase.service';
import { CarnetsConducir, Tablas, Usuario, Vehiculo } from '../../services/tablas.supabase';
import { AuthService } from '@services/auth-service/auth-service';

@Component({
  selector: 'app-admin-users',
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
})
export class AdminUsers implements OnInit {

  constructor(private supabaseService: SupabaseService, private authService: AuthService) { }

  // ── Signals
  finishedLoading = signal(false);
  usuarios = signal<Usuario[]>([]);
  usuariosFiltrados = signal<Usuario[]>([]);
  vehiculos = signal<Vehiculo[]>([]);
  errorMsg = signal('');
  successMsg = signal('');
  modalErrorMsg = signal('');
  modalSuccessMsg = signal('');

  // ── Local
  searchQuery = '';
  showConfirmation = false;
  selected: Usuario | null = Usuario.empty();
  showEditModal = false;
  formData = Usuario.empty();
  opcionesLicencia = Object.keys(CarnetsConducir);

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
    { value: 'Disponible', label: 'Disponible' },
    { value: 'En servicio', label: 'En servicio' },
    { value: 'Inactivo', label: 'Inactivo' },
  ];
  filtrosDisp = new Set<string>();
  dropdownDispAbierto = false;

  // Avatar
  imagenSeleccionada: File | null = null;
  imagenPreview = signal<string | null>(null);
  modalimagen = signal(false);
  cargandoImagen = signal(false);
  avatar_path = "/img/pfp_default.jpg"

  get rol_actual() {
    return this.authService.rol ?? "N"
  }

  async ngOnInit() {
    this.cargarTodo()
  }

  // ── Cierra dropdowns al hacer click fuera
  @HostListener('document:click', ['$event'])
  onDocumentClick(e: MouseEvent) {
    const target = e.target as HTMLElement;
    if (!target.closest('.filtro-dropdown')) {
      this.dropdownRolAbierto = false;
      this.dropdownDispAbierto = false;
    }
  }

  // ── Carga de datos
  async cargarTodo() {
    this.finishedLoading.set(false);
    await this.cargarUsuarios()
    await this.cargarVehiculos()
    this.finishedLoading.set(true);
  }

  async cargarUsuarios() {
    try {
      const data = await this.supabaseService.getAll(Tablas.USUARIOS);
      this.usuarios.set(data.map((v: any) => new Usuario(v)));
      this.filtrar();
    } catch (err: any) {
      this.errorMsg.set('Error al cargar usuarios: ' + err.message);
      console.error('Error al cargar usuarios:', err.message);
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

  // ── Obtener el vehiculo del usuario
  get_vehiculoUsuario(u: Usuario): string {
    let v = this.vehiculos().find(v => v.matricula == u.vehiculo_asignado)
    return v ? `Grúa ${v.marca} — ${v.matricula}` : ""
  }

  // ── Filtrado
  filtrar() {
    const t = this.searchQuery.trim().toLowerCase();

    this.usuariosFiltrados.set(
      this.usuarios().filter(u => {
        // Busqueda por texto
        const matchTexto =
          !t ||
          (u.num_empleado?.toString() ?? '').includes(t) ||
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
    this.dropdownRolAbierto = !this.dropdownRolAbierto;
    this.dropdownDispAbierto = false;
  }

  toggleFiltroRol(value: string) {
    if (this.filtrosRol.has(value)) {
      this.filtrosRol.delete(value);
    } else {
      this.filtrosRol.add(value);
    }
    this.filtrosRol = new Set(this.filtrosRol); // fuerza deteccion de cambio
    this.filtrar();
  }

  limpiarRol() {
    this.filtrosRol = new Set();
    this.filtrar();
  }

  // ── Dropdown Disponibilidad
  toggleDropdownDisp() {
    this.dropdownDispAbierto = !this.dropdownDispAbierto;
    this.dropdownRolAbierto = false;
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
    this.filtrosRol = new Set();
    this.filtrosDisp = new Set();
    this.searchQuery = '';
    this.filtrar();
  }

  // ── Detalles
  abrirDetalles(u: Usuario) {

  }


  // ── Eliminar
  abrirEliminar(u: Usuario) {
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
      await this.cargarTodo();
    } catch (e) {
      this.errorMsg.set('No se pudo eliminar el usuario.');
      console.error(e);
    }
  }

  abrirEditar(u: Usuario) {
    this.formData = new Usuario({ ...u });
    this.modalErrorMsg.set('');
    this.showEditModal = true;
  }

  cerrarEditar() {
    this.showEditModal = false;
  }

  toggleLicencia(lic: string) {
    const licencias = [...(this.formData.licencia_conducir ?? [])];
    const idx = licencias.indexOf(lic);
    if (idx > -1)
      licencias.splice(idx, 1);
    else
      licencias.push(lic);
    this.formData.licencia_conducir = licencias;
  }

  async guardarUsuario() {
    if (!this.formData.nombre) {
      this.modalErrorMsg.set('El nombre es obligatorio');
      return;
    }
    try {
      await this.supabaseService.update(Tablas.USUARIOS, 'id', this.formData.id.toString(), this.formData);

      if (this.imagenSeleccionada) {
        
        //Otro try catch para que si falla la imagen el resto se pueda guardar
        try {
          this.cargandoImagen.set(true);
          await this.supabaseService.subirAvatarUsuario("id", this.formData.id.toString(), this.imagenSeleccionada);
          this.successMsg.set("Usuario con avatar editado!");
        } catch (e: any) {
          console.error("Error al subir avatar:", e);
          this.successMsg.set("Usuario editado con exito");
          this.errorMsg.set("Hubo un problema editando el avatar: "+e.message)
        } finally {
          this.cargandoImagen.set(false);
        }
      }
      else{
        this.successMsg.set("Usuario editado con exito");
      }

      this.cerrarEditar();
      await this.cargarTodo();
    } catch (err: any) {
      this.modalErrorMsg.set('Error al guardar: '+err.message);
    }
  }

  // AVATAR

  cerrarModalImagen() {
    this.modalimagen.set(false)
  }

  // Muestra la imagen en el form
  confirmarImagen() {
    if (this.imagenSeleccionada && this.imagenPreview()) {
      this.avatar_path = this.imagenPreview()!;
      this.modalimagen.set(false);
    }
  }

  onArchivoSeleccionado(event: Event) {
    console.log("clic")
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];

    if (!archivo)
      return;

    // Validar que sea una imagen
    if (!archivo.type.startsWith('image/')) {
      this.errorMsg.set('Selecciona una imagen válida');
      return;
    }

    // Validar tamaño (maximo 5MB)
    const maxSizeMB = 5;
    if (archivo.size > maxSizeMB * 1024 * 1024) {
      this.errorMsg.set(`La imagen no puede superar ${maxSizeMB}MB`);
      return;
    }

    this.errorMsg.set('');
    this.imagenSeleccionada = archivo;

    // Mostrar preview en el modal
    const reader = new FileReader();
    reader.onload = (e) => {
      const preview = e.target?.result as string;
      this.imagenPreview.set(preview);
      this.modalimagen.set(true);
    };
    reader.readAsDataURL(archivo);
  }
}
