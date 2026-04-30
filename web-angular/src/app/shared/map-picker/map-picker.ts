import {
  Component, Output, EventEmitter, Input,
  AfterViewInit, OnDestroy, signal, PLATFORM_ID, Inject
} from '@angular/core';
import { CommonModule, isPlatformBrowser, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAP_COORDENADAS_POR_DEFECTO } from '@services/global/global.service';

@Component({
  selector: 'app-map-picker',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  template: `
    <div class="map-picker-overlay" (click)="onOverlayClick($event)">
      <div class="map-picker-container" (click)="$event.stopPropagation()">

        <div class="map-picker-header">
          <h6 class="mb-0 fw-bold">📍 Seleccionar ubicación</h6>
          <button class="btn-close" (click)="cerrar.emit()"></button>
        </div>

        <div class="map-picker-search">
          <div class="input-group">
            <input
              class="form-control"
              type="text"
              [(ngModel)]="searchQuery"
              placeholder="Buscar dirección..."
              (keydown.enter)="buscar()"
            />
            <button class="btn btn-primary d-flex align-items-center" (click)="buscar()" [disabled]="buscando()">
              @if (buscando()){
                ...
              }
              @else{
                <svg class="text-light" width="20px" height="20px" viewBox="0 0 20 20" stroke="none" fill="currentColor">
                  <path d="M17.545 15.467l-3.779-3.779a6.15 6.15 0 0 0 .898-3.21c0-3.417-2.961-6.377-6.378-6.377A6.185 6.185 0 0 0 2.1 8.287c0 3.416 2.961 6.377 6.377 6.377a6.15 6.15 0 0 0 3.115-.844l3.799 3.801a.953.953 0 0 0 1.346 0l.943-.943c.371-.371.236-.84-.135-1.211zM4.004 8.287a4.282 4.282 0 0 1 4.282-4.283c2.366 0 4.474 2.107 4.474 4.474a4.284 4.284 0 0 1-4.283 4.283c-2.366-.001-4.473-2.109-4.473-4.474z"/>
                </svg>
              }
            </button>
          </div>
          @if (errorBusqueda()) {
            <p class="text-danger small mt-1 mb-0">{{ errorBusqueda() }}</p>
          }
        </div>
        <div id="map-picker-map"></div>
        <div class="map-picker-footer">
          @if (coordenadas()) {
            <div class="d-flex flex-column">
              <span class="text-secondary small">
                📌 Lat: <strong>{{ coordenadas()!.lat | number:'1.6-6' }}</strong>
                &nbsp; Lng: <strong>{{ coordenadas()!.lng | number:'1.6-6' }}</strong>
              </span>
              @if (direccionSeleccionada()) {
                <span class="text-secondary small">{{ direccionSeleccionada() }}</span>
              }
            </div>
            <button class="btn btn-success btn-sm d-flex gap-3 flex-grow-1 align-items-center" (click)="confirmar()">
              <p>✓</p>
              <p class="text-nowrap">Confirmar ubicación</p>
            </button>
          } @else {
            <span class="text-secondary small">Haz clic en el mapa o busca una dirección</span>
          }
        </div>

      </div>
    </div>
  `,
  styles: [`
    .map-picker-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.6);
      z-index: 9999;
      display: flex; align-items: center; justify-content: center;
    }
    .map-picker-container {
      background: #fff;
      border-radius: 12px;
      width: min(700px, 95vw);
      overflow: hidden;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
      display: flex; flex-direction: column;
    }
    .map-picker-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 14px 16px; border-bottom: 1px solid #e5e7eb;
    }
    #map-picker-map{
      height: 380px; width: 100%;
    }
    .map-picker-search { padding: 12px 16px; border-bottom: 1px solid #e5e7eb; }
    .map-picker-footer {
      display: flex; justify-content: space-between; align-items: center;
      padding: 12px 16px; border-top: 1px solid #e5e7eb; min-height: 56px;
      gap: 2rem;
    }
  `]
})
export class MapPickerComponent implements AfterViewInit, OnDestroy {
  // Coordenadas iniciales por defecto (Madrid)
  @Input() lat = MAP_COORDENADAS_POR_DEFECTO.LAT;
  @Input() lng = MAP_COORDENADAS_POR_DEFECTO.LNG;

  // Notifica al padre de que el modal se debe cerrar
  @Output() cerrar = new EventEmitter<void>();

  // Envia las coordenadas al padre
  @Output() ubicacionSeleccionada = new EventEmitter<{
    lat: number; lng: number; direccion: string
  }>();

  searchQuery = '';
  buscando = signal(false);
  errorBusqueda = signal('');
  coordenadas = signal<{ lat: number; lng: number } | null>(null);
  direccionSeleccionada = signal('');

  private map: any = null;
  private marker: any = null;
  private L: any = null;

  // Recibe PLATFORM_ID, que es un token de Angular que indica en que plataforma se esta ejecutando (browser o server)
  constructor(@Inject(PLATFORM_ID) private platformId: Object) { }

  async ngAfterViewInit() {
    // Solo ejecutar en el navegador, nunca en SSR
    if (!isPlatformBrowser(this.platformId)) return;

    // Import dinamico: Leaflet solo se carga en el browser
    this.L = await import('leaflet');
    setTimeout(() => this.initMap(), 50);
  }

  /**
   * El nucleo de la inicializacion del mapa
   *
   * Define el icono del marcador
   *
   * Crea el mapa vinculandolo al div por su id y le pone unas coordenadas por defecto
   *
   * Añade la capa de tiles de 'OpenStreetMap', que son las imagenes cuadradas que formas el mapa.
   * La URL con {s}, {z}, {x}, {y} es una plantilla que Leaflet reconoce para cuadrar el mapa
   */
  private initMap() {

    const iconDefault = this.L.icon({
      iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
    });

    // Coordenadas por defecto
    this.map = this.L.map('map-picker-map').setView(
      [this.lat || MAP_COORDENADAS_POR_DEFECTO.LAT, this.lng || MAP_COORDENADAS_POR_DEFECTO.LNG], 13
    );

    this.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    if (this.lat && this.lng) {
      this.colocarMarcador(this.lat, this.lng, iconDefault);
    }

    // Al hacer click en el mapa, coloca el marcador y llama a nominatim para obtener la direccion
    this.map.on('click', async (e: any) => {
      const { lat, lng } = e.latlng;
      this.colocarMarcador(lat, lng, iconDefault);
      await this.reverseGeocode(lat, lng);
    });
  }

  /**
   * Coloca el icono del marcador en un punto del mapa
   * @param lat latitud
   * @param lng longitud
   * @param icon icono
   */
  private colocarMarcador(lat: number, lng: number, icon: any) {
    const L = this.L;
    if (this.marker) this.map.removeLayer(this.marker);
    // Actualiza el draggable a true para qe pueda ser arrastrado
    this.marker = L.marker([lat, lng], { icon, draggable: true }).addTo(this.map);
    this.coordenadas.set({ lat, lng });

    this.marker.on('dragend', async (e: any) => {
      const pos = e.target.getLatLng();
      this.coordenadas.set({ lat: pos.lat, lng: pos.lng });
      await this.reverseGeocode(pos.lat, pos.lng);
    });
  }

  /**
   * Intenta buscar una direccion.
   *
   * Si lo consigue, mueve el mapa a esa direccion, pone y marcador y guarda la direccion en 'this.direccionSeleccionada'
   */
  async buscar() {
    if (!this.searchQuery.trim())
      return;
    this.buscando.set(true);
    this.errorBusqueda.set('');

    try {
      // 'countrycodes=es' limita las busquedas a españa
      const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(this.searchQuery)}&format=json&limit=1&countrycodes=es`;
      const res = await fetch(url, { headers: { 'Accept-Language': 'es' } });
      const data = await res.json();

      if (!data.length) {
        this.errorBusqueda.set('No se encontró la dirección. Prueba con más detalle.');
        return;
      }

      const { lat, lon, display_name } = data[0];
      const latN = parseFloat(lat), lngN = parseFloat(lon);
      // Mueve el mapa al centro de donde hicimos click
      this.map.setView([latN, lngN], 17);

      // Coloca el marcador en el punto que hicimos click
      const iconDefault = this.L.icon({
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34],
      });
      this.colocarMarcador(latN, lngN, iconDefault);

      this.direccionSeleccionada.set(display_name);
    } catch {
      this.errorBusqueda.set('Error al buscar. Comprueba tu conexión.');
    } finally {
      this.buscando.set(false);
    }
  }

 /**
  * Intenta obtener una direccion segun unas coordenadas, si lo consigue cambia el valor de la variable 'this.direccionSeleccionada'
  * @param lat latitud
  * @param lng longitud
  */
  private async reverseGeocode(lat: number, lng: number) {
    try {
      const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`;
      const res = await fetch(url, { headers: { 'Accept-Language': 'es' } });
      const data = await res.json();
      this.direccionSeleccionada.set(data.display_name ?? '');
    } catch { /* Nada */ }
  }

  /**
   * El punto final del componente
   *
   * Lee el valor de 'this.coordenadas' y se lo emite al padre a traves del 'Output'
   *
   * Despues cierra el modal con el evento 'cerrar'
   */
  confirmar() {
    const c = this.coordenadas();
    if (!c)
      return;
    this.ubicacionSeleccionada.emit({
      lat: c.lat,
      lng: c.lng,
      direccion: this.direccionSeleccionada()
    });
    this.cerrar.emit();
  }

  onOverlayClick(e: MouseEvent) {
    if ((e.target as HTMLElement).classList.contains('map-picker-overlay')) {
      this.cerrar.emit();
    }
  }

  ngOnDestroy() {
    if (this.map) this.map.remove();
  }
}