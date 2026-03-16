import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminCreateuser } from './admin-createuser';

describe('AdminCreateuser', () => {
  let component: AdminCreateuser;
  let fixture: ComponentFixture<AdminCreateuser>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminCreateuser],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminCreateuser);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
