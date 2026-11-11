import { GraphicEntityModule } from './entity-module/GraphicEntityModule.js';
import { TooltipModule } from './tooltip-module/TooltipModule.js';
import { EndScreenModule } from './endscreen-module/EndScreenModule.js';
import { ToggleModule } from './toggle-module/ToggleModule.js';
import { BoardModule } from './board-module/BoardModule.js';

export const modules = [
	GraphicEntityModule,
	TooltipModule,
	EndScreenModule,
	ToggleModule,
	BoardModule
];

// Okabe-Ito player colours (art STYLE), by player id
export const playerColors = ['#56b4e9', '#e69f00', '#009e73', '#cc79a7'];

const onOff = { 'ON': true, 'OFF': false };

// UX 5: one beam toggle per player, yards, lock rings, coordinates
export const options = [
	ToggleModule.defineToggle({ toggle: 'beam0', title: 'BEAMS: SKY (PLAYER 1)', values: onOff, default: true }),
	ToggleModule.defineToggle({ toggle: 'beam1', title: 'BEAMS: AMBER (PLAYER 2)', values: onOff, default: true }),
	ToggleModule.defineToggle({ toggle: 'beam2', title: 'BEAMS: JADE (PLAYER 3)', values: onOff, default: true }),
	ToggleModule.defineToggle({ toggle: 'beam3', title: 'BEAMS: ORCHID (PLAYER 4)', values: onOff, default: true }),
	ToggleModule.defineToggle({ toggle: 'yards', title: 'YARDS', values: onOff, default: true }),
	ToggleModule.defineToggle({ toggle: 'locks', title: 'LOCK RINGS', values: onOff, default: true }),
	ToggleModule.defineToggle({ toggle: 'coords', title: 'COORDINATES', values: onOff, default: true })
];
