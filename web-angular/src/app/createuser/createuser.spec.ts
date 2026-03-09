import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Createuser } from './createuser';

describe('Createuser', () => {
  let component: Createuser;
  let fixture: ComponentFixture<Createuser>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Createuser]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Createuser);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
