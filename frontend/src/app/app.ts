import { Component } from '@angular/core';
import { UploadComponent } from './components/upload/upload';
import { LocateComponent } from './components/locate/locate';
import { StrategyComponent } from "./components/strategy/strategy";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [UploadComponent, LocateComponent, StrategyComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  title = 'NavVis Building Locator';
}
