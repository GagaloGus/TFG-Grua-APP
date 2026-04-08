import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminVehiculos } from './admin-vehiculos';

describe('AdminVehiculos', () => {
  let component: AdminVehiculos;
  let fixture: ComponentFixture<AdminVehiculos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminVehiculos],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminVehiculos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
