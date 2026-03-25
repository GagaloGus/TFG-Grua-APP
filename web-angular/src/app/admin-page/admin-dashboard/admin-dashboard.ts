import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-dashboard',
  imports: [FormsModule],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboard {
  activeServices: number = 0;
  completedServices: number = 0;
  availableVehicles: number = 0;
  availableDrivers: number = 0;
}
