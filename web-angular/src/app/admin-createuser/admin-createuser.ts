import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-createuser',
  imports: [FormsModule],
  standalone: true,
  templateUrl: './admin-createuser.html',
  styleUrl: './admin-createuser.css',
})
export class AdminCreateuser {
createNewUser() {
throw new Error('Method not implemented.');
}
password: any;
username: any;
}
