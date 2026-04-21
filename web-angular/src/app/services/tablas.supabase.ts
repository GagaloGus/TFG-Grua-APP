export enum Tablas {
  USUARIOS = 'usuarios',
  VEHICULOS = 'vehiculos',
  SERVICIOS = 'servicios'
}

export enum Disponibilidad{
  DISPONIBLE = 'Disponible',
  EN_SERVICIO = 'En servicio',
  OCUPADO = 'Ocupado',
  INACTIVO = 'Inactivo',
}

export enum Estado{
  SIN_EMPEZAR = 'Sin empezar',
  EN_CURSO = 'En curso',
  TERMINADO = 'Terminado',
  CANCELADO = 'Cancelado',
}

export enum CarnetsConducir {
  A = 'A',
  A1 = 'A1',
  A2 = 'A2',
  B = 'B',
  B1 = 'B1',
  C = 'C',
  C1 = 'C1',
  D = 'D',
  D1 = 'D1',
  'B+E' = 'B+E',
  'C+E' = 'C+E',
  'D+E' = 'D+E',
}

export class Vehiculo {
  id!: number;
  matricula!: string;
  disponible!: boolean;
  activo!: boolean;
  marca!: string | null;
  zona_trabajo!: string | null;
  litro_combustible_km!: number;

  constructor(data: Partial<Vehiculo> = {}) {
    Object.assign(this, data);
  }

  static empty(): Vehiculo {
    return new Vehiculo({
      disponible: false,
      activo: false,
      marca: null,
      zona_trabajo: null,
      litro_combustible_km: 0
    });
  }
}

export class Servicio {
  id!: number;
  direccion_ubi_recogida!: string | null;
  ubicacion_recogida_lat: number = 0;
  ubicacion_recogida_lng: number = 0;
  direccion_ubi_destino!: string | null;
  ubicacion_destino_lat: number = 0;
  ubicacion_destino_lng: number = 0;
  fecha!: string;
  costo: number = 0;
  ingresos: number = 0;
  num_empleado!: number | null;
  vehiculo_matricula!: string | null;
  observaciones!: string | null;
  estado!: Estado;
  tel_cliente!: string | null;
  nombre_cliente!: string | null;
  vehiculo_recogido: boolean = false;

  constructor(data: Partial<Servicio> = {}) {
    Object.assign(this, data);
  }

  static empty(): Servicio {
    return new Servicio({
      direccion_ubi_recogida: null,
      direccion_ubi_destino: null,
      estado: Estado.SIN_EMPEZAR,
      num_empleado: null,
      vehiculo_matricula: null,
      observaciones: null,
      tel_cliente: null,
      nombre_cliente: null,
    });
  }

  get estadoClass(): string {
    const map: Record<string, string> = {
      'Sin empezar': 'badge-estado-sin-empezar',
      'En curso': 'badge-estado-en-curso',
      'Terminado': 'badge-estado-terminado',
      'Cancelado': 'badge-estado-cancelado',
    };
    return map[this.estado] ?? '';
  }

  get recogidoClass():string{
    return this.vehiculo_recogido ? 'badge-estado-terminado' : 'badge-estado-borrado'
  }

  get formatCoordRecogida(): string {
    return `${this.ubicacion_recogida_lat.toFixed(4)}, ${this.ubicacion_recogida_lng.toFixed(4)}`
  }

  get formatCoordDestino(): string {
    return `${this.ubicacion_destino_lat.toFixed(4)}, ${this.ubicacion_destino_lng.toFixed(4)}`
  }

  get distancia_real(){
    return this.calcularDistanciaTierra(this.ubicacion_recogida_lat, this.ubicacion_recogida_lng, this.ubicacion_destino_lat, this.ubicacion_destino_lng)
  }

  calcularDistanciaTierra(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const deg2rad = (Math.PI / 180);
    const R = 6371; // radio de la tierra en km
    const dLat = (lat2 - lat1) * deg2rad;
    const dLon = (lng2 - lng1) * deg2rad;

    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * deg2rad) * Math.cos(lat2 * deg2rad) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c; // Distancia en km
  }
}

export class Usuario {
  id!: number;
  num_empleado!: number;
  vehiculo_asignado!: string | null;
  created_at?: string;
  nombre!: string;
  apellido1!: string;
  apellido2!: string;
  password!: string;
  rol!: string;
  disponibilidad!: Disponibilidad;
  serv_completados_total: number = 0;
  serv_completados_hoy: number = 0;
  licencia_conducir: string[] = [];
  telefono!: string | null;
  email!: string | null;
  avatar_url!: string | null;

  constructor(data: Partial<Usuario> = {}) {
    Object.assign(this, data);
  }

  static empty(): Usuario {
    return new Usuario({});
  }

  get nombreCompleto(): string {
    return `${this.nombre ?? ""} ${this.apellido1 ?? ""} ${this.apellido2 ?? ""}`.trim();
  }

  get licenciasFormateadas(): string {
    return this.licencia_conducir?.join('+') ?? '';
  }

  get disponibilidadCSSClass(): string {
    if (this.disponibilidad == "En servicio") return "chip-text-blue"
    else if (this.disponibilidad == "Disponible") return "chip-text-green"
    else if (this.disponibilidad == "Inactivo") return "chip-text-grey"
    return "chip-text"
  }

  get rolNombre(): string {
    if (this.rol == "N") return "Sin rol"
    else if (this.rol == "A") return "ADMIN"
    else if (this.rol == "U") return "Usuario"
    else if (this.rol == "T") return "Trabajador"
    return "Rol indefinido"
  }

  get rolCSSClass(): string {
    if (this.rol == "N") return "chip-text-grey"
    else if (this.rol == "A") return "chip-text-purple"
    else if (this.rol == "U") return "chip-text-blue"
    else if (this.rol == "T") return "chip-text-red"
    return "chip-text"
  }
}