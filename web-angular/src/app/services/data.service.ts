import { Injectable, signal } from "@angular/core";
import { Usuario, Vehiculo, Servicio, Tablas } from "./tablas.supabase";
import { SupabaseService } from "./supabase.service";

@Injectable({ providedIn: 'root' })
export class DataService {

    private can_cache_data = false;
    private _usuarios = signal<Usuario[]>([]);
    private _vehiculos = signal<Vehiculo[]>([]);
    private _servicios = signal<Servicio[]>([]);

    readonly usuarios = this._usuarios.asReadonly();
    readonly vehiculos = this._vehiculos.asReadonly();
    readonly servicios = this._servicios.asReadonly();

    constructor(private supabase: SupabaseService) { }

    async cargarUsuarios() {
        try {
            if (this.can_cache_data) {
                if (this._usuarios().length > 0)
                    return;
            }
            const data = await this.supabase.getAll(Tablas.USUARIOS);
            this._usuarios.set(data.map((u: any) => new Usuario(u)));
        } catch (err: any) {
            throw Error(`Error al cargar usuarios: ${err.message}`);
        }
    }

    async cargarVehiculos() {
        try {
            if (this.can_cache_data) {
                if (this._vehiculos().length > 0)
                    return;
            }
            const data = await this.supabase.getAll(Tablas.VEHICULOS);
            this._vehiculos.set(data.map((v: any) => new Vehiculo(v)));
        } catch (err: any) {
            throw Error(`Error al cargar vehiculos: ${err.message}`);
        }
    }

    async cargarServicios() {
        try {
            if (this.can_cache_data) {
                if (this._servicios().length > 0)
                    return;
            }
            const data = await this.supabase.getAll(Tablas.SERVICIOS);
            this._servicios.set(data.map((s: any) => new Servicio(s)));
        } catch (err: any) {
            throw Error(`Error al cargar servicios: ${err.message}`);
        }
    }

    // fuerza recarga
    invalidarServicios() { this._servicios.set([]); }
    invalidarVehiculos() { this._vehiculos.set([]); }
    invalidarUsuarios() { this._usuarios.set([]); }
}