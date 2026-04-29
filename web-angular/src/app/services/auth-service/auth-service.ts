import { Injectable, signal, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { SupabaseService } from '../supabase.service';
import { Tablas, Usuario } from '../tablas.supabase';

const SESSION_KEY = 'app_session';

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private _isLoggedIn = false;
  private _isAdmin = false;
  private _currentUsuario: Usuario | null = null;
  avatarUrl = signal<string | null>(null);

  constructor(
    private supabaseService: SupabaseService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    // Al iniciar, intentamos restaurar la sesion guardada
    this.restaurarSesion();
  }

  // ── Persistencia ─────────────────────────────────────────────────

  private restaurarSesion() {
    if (!isPlatformBrowser(this.platformId)) return;

    // Busca en localStorage (variable 'recordarSesion') o sessionStorage (sesion normal)
    const raw =
      localStorage.getItem(SESSION_KEY) ??
      sessionStorage.getItem(SESSION_KEY);

    if (!raw) return;

    try {
      const usuario = new Usuario(JSON.parse(raw));
      this._currentUsuario = usuario;
      this._isLoggedIn = true;
      this.cargarAvatar();
      console.log(`Sesion restaurada! -> ${this._currentUsuario.email}`)
    } catch {
      // Datos corruptos o error -> limpiar
      localStorage.removeItem(SESSION_KEY);
      sessionStorage.removeItem(SESSION_KEY);
    }
  }

  private guardarSesion(recordarSesion: boolean) {
    if (!isPlatformBrowser(this.platformId)) return;
    const datos = JSON.stringify(this._currentUsuario);

    if (recordarSesion) {
      // localStorage persiste aunque se cierre el navegador
      localStorage.setItem(SESSION_KEY, datos);
    } else {
      // sessionStorage se borra al cerrar la pestaña
      sessionStorage.setItem(SESSION_KEY, datos);
    }
  }

  private limpiarSesion() {
    if (!isPlatformBrowser(this.platformId)) return;
    localStorage.removeItem(SESSION_KEY);
    sessionStorage.removeItem(SESSION_KEY);
  }

  // ── Login / Logout ────────────────────────────────────────────────

  async login(email: string, password: string, recordarSesion = false): Promise<string> {
    if (email == '') return "El campo 'Correo' está vacío!";
    if (password == '') return "El campo 'Contraseña' está vacío!";

    try {
      const match = await this.supabaseService.find(Tablas.USUARIOS, 'email', email);

      if (match.length === 0)
        throw new Error(`No existe el usuario '${email}'`);

      if (match[0]['email'] == email && match[0]['password'] == password) {
        this._currentUsuario = new Usuario(match[0]);
        await this.cargarAvatar();
        this._isLoggedIn = true;
        this.guardarSesion(recordarSesion);  // <- guarda si el checkbox esta marcado
      } else {
        throw new Error('La contraseña no coincide!');
      }
    } catch (error: any) {
      return error.message;
    }

    return '';
  }

  logout() {
    this._currentUsuario = null;
    this._isLoggedIn = false;
    this._isAdmin = false;
    this.avatarUrl.set(null);
    this.limpiarSesion();   // <- borra de storage al cerrar sesion
  }

  // ── Getters ───────────────────────────────────────────────────────

  isLoggedIn(): boolean { return this._isLoggedIn; }
  isAdmin(): boolean { return this._isAdmin; }

  get nombre() { return this._currentUsuario?.nombre || null; }
  get apellido1() { return this._currentUsuario?.apellido1 || null; }
  get email() { return this._currentUsuario?.email || null; }
  get rol() { return this._currentUsuario?.rol ?? null; }

  async cargarAvatar() {
    const url = this._currentUsuario?.avatar_url;
    if (!url) { this.avatarUrl.set(null); return; }
    try {
      const res = await fetch(url, { method: 'HEAD' });
      this.avatarUrl.set(res.ok ? url : null);
    } catch {
      this.avatarUrl.set(null);
    }
  }
}