import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api';

@Component({
  selector: 'app-strategy',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './strategy.html',
  styleUrl: './strategy.css',
})
export class StrategyComponent implements OnInit {
  current: 'JAVA' | 'POSTGIS' = 'JAVA';
  switching = false;

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.api.getStrategy().subscribe({
      next: (res) => {
        this.current = res.strategy;
        this.cdr.markForCheck();
      },
    });
  }

  toggle() {
    const next = this.current === 'JAVA' ? 'POSTGIS' : 'JAVA';
    this.switching = true;

    this.api.setStrategy(next).subscribe({
      next: (res) => {
        this.current = res.strategy;
        this.switching = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.switching = false;
        this.cdr.markForCheck();
      },
    });
  }
}
